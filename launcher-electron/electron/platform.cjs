const { spawnSync } = require("node:child_process");
const fs = require("node:fs");
const path = require("node:path");

function platformKey(platform = process.platform, arch = process.arch) {
  const osKey = platform === "win32"
    ? "windows"
    : platform === "darwin" ? "macos" : platform === "linux" ? "linux" : platform;
  const archKey = arch === "x64" || arch === "arm64" ? arch : arch.replace(/[^a-zA-Z0-9_-]/g, "");
  return `${osKey}-${archKey || "unknown"}`;
}

function scriptName(baseName, platform = process.platform) {
  return platform === "win32" ? `${baseName}.bat` : baseName;
}

function javaBinaryName(platform = process.platform) {
  return platform === "win32" ? "java.exe" : "java";
}

function gradleCommand(projectRoot, platform = process.platform) {
  const wrapper = path.join(projectRoot, scriptName("gradlew", platform));
  ensureExecutable(wrapper, platform);
  return platform === "win32" ? wrapper : "./gradlew";
}

function gameExecutable(root, name, platform = process.platform) {
  const executable = path.join(root, name, "bin", scriptName(name, platform));
  ensureExecutable(executable, platform);
  return executable;
}

function runtimeInfo(options = {}) {
  const runtime = resolveJavaRuntime(options);
  if (!runtime.ok) {
    return {
      ok: false,
      command: "",
      javaHome: "",
      source: "missing",
      platform: platformKey(options.platform, options.arch),
      version: "",
      message: "Java 21 wurde nicht gefunden. Bitte JDK 21 installieren, ADVENTURA_JAVA_HOME setzen oder eine gebuendelte Runtime bereitstellen."
    };
  }
  return {
    ok: true,
    command: runtime.command,
    javaHome: runtime.javaHome || "",
    source: runtime.source,
    platform: platformKey(options.platform, options.arch),
    version: runtime.version,
    message: runtime.version
  };
}

function resolveJavaRuntime(options = {}) {
  const candidates = javaCandidates(options);
  const failures = [];
  for (const candidate of candidates) {
    if (candidate.command !== "java" && !fs.existsSync(candidate.command)) {
      failures.push(`${candidate.source}: missing`);
      continue;
    }
    ensureExecutable(candidate.command, options.platform || process.platform);
    const result = spawnSync(candidate.command, ["-version"], {
      encoding: "utf8",
      timeout: 5000
    });
    if (result.error) {
      failures.push(`${candidate.source}: ${result.error.message}`);
      continue;
    }
    const output = `${result.stderr || ""}\n${result.stdout || ""}`.trim();
    const firstLine = output.split(/\r?\n/).find(Boolean) || "java -version";
    const major = parseJavaMajorVersion(firstLine);
    if (major >= 21) {
      return {
        ok: true,
        command: candidate.command,
        javaHome: candidate.javaHome || "",
        source: candidate.source,
        version: firstLine
      };
    }
    failures.push(`${candidate.source}: ${firstLine}`);
  }
  return { ok: false, failures };
}

function javaCandidates(options = {}) {
  const env = options.env || process.env;
  const platform = options.platform || process.platform;
  const resourcesPath = options.resourcesPath || "";
  const projectRoot = options.projectRoot || "";
  const key = platformKey(platform, options.arch || process.arch);
  const candidates = [];

  addJavaHomeCandidate(candidates, env.ADVENTURA_JAVA_HOME, "ADVENTURA_JAVA_HOME", platform);
  if (resourcesPath) {
    addJavaHomeCandidate(candidates, path.join(resourcesPath, "runtime", key), `bundled runtime ${key}`, platform);
    addJavaHomeCandidate(candidates, path.join(resourcesPath, "runtime"), "bundled runtime", platform);
  }
  if (projectRoot) {
    addJavaHomeCandidate(candidates, path.join(projectRoot, ".runtime", key), `.runtime ${key}`, platform);
    addJavaHomeCandidate(candidates, path.join(projectRoot, ".runtime"), ".runtime", platform);
  }
  detectInstalledJavaHomes(env, platform)
    .forEach((home) => addJavaHomeCandidate(candidates, home, "system install", platform));
  addJavaHomeCandidate(candidates, env.JAVA_HOME, "JAVA_HOME", platform);
  candidates.push({ source: "PATH", command: "java", javaHome: "" });
  return dedupeCandidates(candidates);
}

function detectInstalledJavaHomes(env = process.env, platform = process.platform) {
  const homes = [];
  if (platform === "win32") {
    const home = env.USERPROFILE || "";
    [
      env.ProgramFiles && path.join(env.ProgramFiles, "Java"),
      env.ProgramFiles && path.join(env.ProgramFiles, "Eclipse Adoptium"),
      env.ProgramFiles && path.join(env.ProgramFiles, "Microsoft"),
      env["ProgramFiles(x86)"] && path.join(env["ProgramFiles(x86)"], "Java"),
      home && path.join(home, ".jdks"),
      home && path.join(home, "scoop", "apps")
    ].filter(Boolean).forEach((root) => collectJavaHomes(root, platform, homes));
  } else if (platform === "darwin") {
    [
      "/Library/Java/JavaVirtualMachines",
      path.join(env.HOME || "", "Library", "Java", "JavaVirtualMachines")
    ].filter(Boolean).forEach((root) => collectJavaHomes(root, platform, homes));
  } else {
    [
      "/usr/lib/jvm",
      "/usr/java",
      "/opt/java",
      path.join(env.HOME || "", ".jdks")
    ].filter(Boolean).forEach((root) => collectJavaHomes(root, platform, homes));
  }

  return homes.sort(compareJavaHomePreference);
}

function collectJavaHomes(root, platform, homes, depth = 0) {
  if (!root || depth > 2 || !fs.existsSync(root)) {
    return;
  }
  const javaHome = normalizeJavaHome(root, platform);
  if (javaHome) {
    homes.push(javaHome);
  }

  let children;
  try {
    children = fs.readdirSync(root, { withFileTypes: true });
  } catch (_error) {
    return;
  }

  for (const child of children) {
    if (!child.isDirectory()) {
      continue;
    }
    collectJavaHomes(path.join(root, child.name), platform, homes, depth + 1);
  }
}

function normalizeJavaHome(candidate, platform) {
  const javaBinary = path.join(candidate, "bin", javaBinaryName(platform));
  if (fs.existsSync(javaBinary)) {
    return candidate;
  }
  const macHome = path.join(candidate, "Contents", "Home");
  if (fs.existsSync(path.join(macHome, "bin", javaBinaryName(platform)))) {
    return macHome;
  }
  return "";
}

function compareJavaHomePreference(left, right) {
  const leftScore = javaHomePreferenceScore(left);
  const rightScore = javaHomePreferenceScore(right);
  if (leftScore !== rightScore) {
    return rightScore - leftScore;
  }
  return left.localeCompare(right);
}

function javaHomePreferenceScore(javaHome) {
  const name = path.basename(javaHome).toLowerCase();
  const major = Number.parseInt(name.match(/(?:jdk-|jdk|temurin-|openjdk-?)(\d+)/)?.[1] || name.match(/\b(\d{2})\b/)?.[1] || "0", 10);
  if (major === 21) {
    return 400;
  }
  if (major > 21) {
    return 300 - Math.min(major - 21, 50);
  }
  if (major >= 17) {
    return 100 + major;
  }
  return major;
}

function addJavaHomeCandidate(candidates, javaHome, source, platform) {
  if (!javaHome || typeof javaHome !== "string") {
    return;
  }
  const homes = [
    javaHome,
    path.join(javaHome, "Contents", "Home")
  ];
  for (const home of homes) {
    candidates.push({
      source,
      command: path.join(home, "bin", javaBinaryName(platform)),
      javaHome: home
    });
  }
}

function dedupeCandidates(candidates) {
  const seen = new Set();
  return candidates.filter((candidate) => {
    const key = `${candidate.source}:${candidate.command}`;
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
}

function parseJavaMajorVersion(line) {
  const version = line.match(/version "(?<version>[^"]+)"/)?.groups?.version
    || line.match(/openjdk (?<version>[0-9][^\s]*)/)?.groups?.version
    || "";
  if (version.startsWith("1.")) {
    return Number.parseInt(version.slice(2), 10) || 0;
  }
  return Number.parseInt(version, 10) || 0;
}

function ensureExecutable(filePath, platform = process.platform) {
  if (platform === "win32" || !filePath || filePath === "java" || !fs.existsSync(filePath)) {
    return false;
  }
  const stat = fs.statSync(filePath);
  const desiredMode = stat.mode | 0o755;
  if (stat.mode !== desiredMode) {
    fs.chmodSync(filePath, desiredMode);
    return true;
  }
  return false;
}

function environmentForRuntime(runtime, baseEnv = process.env) {
  const env = { ...baseEnv };
  const pathKey = Object.keys(env).find((key) => key.toLowerCase() === "path") || "PATH";
  const currentPath = env[pathKey] || "";
  env.ADVENTURA_PLATFORM = runtime.platform || platformKey();
  if (runtime.javaHome) {
    env.JAVA_HOME = runtime.javaHome;
    env[pathKey] = `${path.join(runtime.javaHome, "bin")}${path.delimiter}${currentPath}`;
  } else if (runtime.command && path.isAbsolute(runtime.command)) {
    env[pathKey] = `${path.dirname(runtime.command)}${path.delimiter}${currentPath}`;
  }
  return env;
}

module.exports = {
  environmentForRuntime,
  gameExecutable,
  gradleCommand,
  javaCandidates,
  parseJavaMajorVersion,
  platformKey,
  resolveJavaRuntime,
  runtimeInfo,
  scriptName
};

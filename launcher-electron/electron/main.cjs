const { app, BrowserWindow, ipcMain, shell } = require("electron");
const { spawn, spawnSync } = require("node:child_process");
const fs = require("node:fs");
const fsp = require("node:fs/promises");
const os = require("node:os");
const path = require("node:path");

const devProjectRoot = path.resolve(__dirname, "..", "..");
const bundledGameRoot = path.join(process.resourcesPath, "game");
const settingsPath = path.join(os.homedir(), ".adventura", "launcher.properties");
const packagedRuntimeRoot = path.join(os.homedir(), ".adventura", "runtime");
const running = new Map();
const sourceModules = {
  client: ["common", "client"],
  server: ["common", "server"]
};

const defaults = {
  username: "Player",
  seed: "1337",
  renderDistance: 8,
  previewRadius: 3,
  host: "127.0.0.1",
  port: 25565,
  autoStartServer: false
};

if (process.platform === "linux") {
  app.commandLine.appendSwitch("no-sandbox");
  app.commandLine.appendSwitch("disable-gpu");
  app.commandLine.appendSwitch("disable-gpu-sandbox");
  app.disableHardwareAcceleration();
}

function createWindow() {
  const win = new BrowserWindow({
    title: "Adventura Launcher",
    width: 1220,
    height: 780,
    minWidth: 1040,
    minHeight: 680,
    backgroundColor: "#030508",
    titleBarStyle: process.platform === "darwin" ? "hiddenInset" : "default",
    webPreferences: {
      preload: path.join(__dirname, "preload.cjs"),
      contextIsolation: true,
      nodeIntegration: false
    }
  });

  if (process.env.ELECTRON_RENDERER_URL) {
    win.loadURL(process.env.ELECTRON_RENDERER_URL);
  } else {
    win.loadFile(path.join(__dirname, "..", "dist", "index.html"));
  }
}

app.whenReady().then(createWindow);

app.on("window-all-closed", () => {
  stopAllProcesses();
  if (process.platform !== "darwin") {
    app.quit();
  }
});

app.on("before-quit", stopAllProcesses);

app.on("activate", () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});

ipcMain.handle("runtime:getInfo", () => ({
  projectRoot: launcherWorkspaceRoot(),
  launchMode: app.isPackaged ? "bundled" : "installDist",
  java: javaRuntimeInfo(),
  settingsPath,
  gradle: gradleCommand(),
  packaged: app.isPackaged
}));

ipcMain.handle("workspace:open", async () => {
  await shell.openPath(launcherWorkspaceRoot());
  return { ok: true };
});

ipcMain.handle("settings:load", async () => loadSettings());

ipcMain.handle("settings:save", async (_event, settings) => {
  const sanitized = sanitizeSettings(settings);
  await saveSettings(sanitized);
  broadcastLog("Settings gespeichert.");
  return sanitized;
});

ipcMain.handle("launcher:launch", async (_event, payload) => {
  const settings = sanitizeSettings(payload?.settings || defaults);
  await saveSettings(settings);

  if (payload?.mode === "server") {
    return startServer(settings);
  }

  if (payload?.mode === "multiplayer" && settings.autoStartServer) {
    const serverResult = await startServer(settings);
    if (!serverResult.ok && !serverResult.alreadyRunning) {
      return serverResult;
    }
  }

  if (payload?.mode === "multiplayer") {
    return startClient("Multiplayer", clientArgs(settings, true));
  }

  return startClient("Singleplayer", clientArgs(settings, false));
});

ipcMain.handle("launcher:preflight", async (_event, payload) => preflight(payload?.settings || defaults));

ipcMain.handle("launcher:stop", (_event, target) => {
  if (target === "all") {
    stopAllProcesses();
    broadcastLog("Alle Launcher-Prozesse wurden gestoppt.");
    return { ok: true };
  }
  const child = running.get(target);
  if (!child) {
    return { ok: false, message: "Kein laufender Prozess gefunden." };
  }
  child.kill();
  running.delete(target);
  broadcastLog(`${target} wurde gestoppt.`);
  return { ok: true };
});

async function loadSettings() {
  try {
    const file = await fsp.readFile(settingsPath, "utf8");
    return sanitizeSettings(parseProperties(file));
  } catch (error) {
    if (error.code !== "ENOENT") {
      broadcastLog(`Settings konnten nicht geladen werden: ${error.message}`);
    }
    return { ...defaults };
  }
}

async function saveSettings(settings) {
  await fsp.mkdir(path.dirname(settingsPath), { recursive: true });
  const lines = [
    "# Adventura launcher settings",
    `username=${escapeProperty(settings.username)}`,
    `seed=${escapeProperty(settings.seed)}`,
    `renderDistance=${settings.renderDistance}`,
    `previewRadius=${settings.previewRadius}`,
    `host=${escapeProperty(settings.host)}`,
    `port=${settings.port}`,
    `autoStartServer=${settings.autoStartServer}`
  ];
  await fsp.writeFile(settingsPath, `${lines.join(os.EOL)}${os.EOL}`, "utf8");
}

function parseProperties(file) {
  return file.split(/\r?\n/).reduce((properties, line) => {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) {
      return properties;
    }
    const separator = trimmed.search(/[:=]/);
    if (separator === -1) {
      return properties;
    }
    const key = trimmed.slice(0, separator).trim();
    const value = trimmed.slice(separator + 1).trim();
    properties[key] = value.replace(/\\:/g, ":").replace(/\\=/g, "=").replace(/\\\\/g, "\\");
    return properties;
  }, {});
}

function sanitizeSettings(input) {
  return {
    username: clampText(input.username, defaults.username, 32),
    seed: normalizeInteger(input.seed, defaults.seed, Number.MIN_SAFE_INTEGER, Number.MAX_SAFE_INTEGER).toString(),
    renderDistance: normalizeInteger(input.renderDistance, defaults.renderDistance, 2, 18),
    previewRadius: normalizeInteger(input.previewRadius, defaults.previewRadius, 1, 8),
    host: clampText(input.host, defaults.host, 255),
    port: normalizeInteger(input.port, defaults.port, 1, 65535),
    autoStartServer: input.autoStartServer === true || input.autoStartServer === "true"
  };
}

function clampText(value, fallback, maxLength) {
  const text = typeof value === "string" ? value.trim() : "";
  return text && text.length <= maxLength ? text : fallback;
}

function normalizeInteger(value, fallback, min, max) {
  const parsed = Number.parseInt(String(value), 10);
  if (!Number.isFinite(parsed)) {
    return fallback;
  }
  return Math.max(min, Math.min(max, parsed));
}

function escapeProperty(value) {
  return String(value).replace(/\\/g, "\\\\").replace(/=/g, "\\=").replace(/:/g, "\\:");
}

function clientArgs(settings, multiplayer) {
  if (multiplayer) {
    return [
      "--auto-join",
      "--connect", settings.host,
      "--port", String(settings.port),
      "--username", settings.username,
      "--preview-radius", String(settings.previewRadius),
      "--render-distance", String(settings.renderDistance)
    ];
  }

  return [
    "--auto-singleplayer",
    "--seed", settings.seed,
    "--preview-radius", String(settings.previewRadius),
    "--render-distance", String(settings.renderDistance)
  ];
}

function serverArgs(settings) {
  return [
    "--port", String(settings.port),
    "--seed", settings.seed,
    "--whitelist", settings.username
  ];
}

async function startClient(label, args) {
  const javaCheck = ensureJavaRuntime();
  if (javaCheck) {
    return javaCheck;
  }
  const runningCheck = alreadyRunning("client");
  if (runningCheck) {
    return runningCheck;
  }
  if (app.isPackaged) {
    return startGameProcess("client", bundledExecutable("client"), args, `${label} startet.`, packagedRuntimeRoot);
  }
  const prepared = await ensureDevDistribution("client");
  if (!prepared.ok) {
    return prepared;
  }
  return startGameProcess("client", devExecutable("client"), args, `${label} startet.`, devProjectRoot);
}

async function startServer(settings) {
  const javaCheck = ensureJavaRuntime();
  if (javaCheck) {
    return javaCheck;
  }
  const runningCheck = alreadyRunning("server");
  if (runningCheck) {
    return runningCheck;
  }
  const args = serverArgs(settings);
  if (app.isPackaged) {
    return startGameProcess("server", bundledExecutable("server"), args, `Server startet auf ${settings.host}:${settings.port}.`, packagedRuntimeRoot);
  }
  const prepared = await ensureDevDistribution("server");
  if (!prepared.ok) {
    return prepared;
  }
  return startGameProcess("server", devExecutable("server"), args, `Server startet auf ${settings.host}:${settings.port}.`, devProjectRoot);
}

function preflight(inputSettings) {
  const settings = sanitizeSettings(inputSettings);
  const java = javaRuntimeInfo();
  const checks = [
    {
      label: "Java 21",
      status: java.ok ? "ok" : "error",
      detail: java.ok ? java.version : java.message
    },
    {
      label: "Settings",
      status: "ok",
      detail: `${settings.username} / ${settings.host}:${settings.port} / ${settings.renderDistance} Chunks`
    }
  ];

  if (app.isPackaged) {
    checks.push(executableCheck("Client Starter", bundledExecutable("client"), true));
    checks.push(executableCheck("Server Starter", bundledExecutable("server"), true));
  } else {
    checks.push(gradleWrapperCheck());
    checks.push(distributionCheck("client"));
    checks.push(distributionCheck("server"));
  }

  const ok = checks.every((check) => check.status !== "error");
  broadcastLog(ok ? "Preflight ok: Client kann starten." : "Preflight Fehler: Terminal pruefen.", "preflight", ok ? "ok" : "error");
  return { ok, checks };
}

function gradleWrapperCheck() {
  const wrapper = path.join(devProjectRoot, gradleCommand());
  return {
    label: "Gradle Wrapper",
    status: fs.existsSync(wrapper) ? "ok" : "error",
    detail: fs.existsSync(wrapper) ? wrapper : `${gradleCommand()} wurde nicht gefunden.`
  };
}

function distributionCheck(name) {
  const label = name === "client" ? "Client Starter" : "Server Starter";
  const executable = devExecutable(name);
  if (!fs.existsSync(executable)) {
    return {
      label,
      status: "warn",
      detail: "Noch nicht gebaut. Wird beim Start automatisch mit Gradle installDist erzeugt."
    };
  }
  if (!isDevDistributionFresh(name)) {
    return {
      label,
      status: "warn",
      detail: "Veraltet. Wird beim Start automatisch neu gebaut."
    };
  }
  return {
    label,
    status: "ok",
    detail: executable
  };
}

function executableCheck(label, executable, required) {
  const exists = fs.existsSync(executable);
  return {
    label,
    status: exists ? "ok" : required ? "error" : "warn",
    detail: exists ? executable : `${executable} fehlt.`
  };
}

function alreadyRunning(key) {
  const current = running.get(key);
  if (current && current.exitCode === null) {
    const message = `${key === "client" ? "Client" : "Server"} laeuft bereits.`;
    broadcastLog(message);
    return { ok: false, alreadyRunning: true, message };
  }
  return null;
}

async function ensureDevDistribution(name) {
  const label = name === "client" ? "Client" : "Server";
  const task = `:${name}:installDist`;
  const executable = devExecutable(name);

  if (isDevDistributionFresh(name)) {
    broadcastLog(`${label}-Starter ist aktuell.`);
    return { ok: true };
  }

  broadcastLog(`${label} wird vorbereitet (${task}).`);
  const result = await runCommand(gradleCommand(), [task], devProjectRoot, "gradle");
  if (!result.ok) {
    const message = `${label} konnte nicht vorbereitet werden (${result.detail}).`;
    broadcastLog(message);
    return { ok: false, message };
  }

  if (!fs.existsSync(executable)) {
    const message = `${label}-Starter wurde nicht erzeugt: ${executable}`;
    broadcastLog(message);
    return { ok: false, message };
  }

  return { ok: true };
}

function isDevDistributionFresh(name) {
  const executable = devExecutable(name);
  if (!fs.existsSync(executable)) {
    return false;
  }

  const outputTime = newestModifiedTime([devDistributionRoot(name)]);
  const inputTime = newestModifiedTime(devInputPaths(name));
  return outputTime > 0 && outputTime >= inputTime;
}

function devInputPaths(name) {
  const modules = sourceModules[name] || [name];
  const modulePaths = modules.flatMap((moduleName) => [
    path.join(devProjectRoot, moduleName, "src", "main"),
    path.join(devProjectRoot, moduleName, "build.gradle.kts")
  ]);

  return [
    path.join(devProjectRoot, "build.gradle.kts"),
    path.join(devProjectRoot, "settings.gradle.kts"),
    path.join(devProjectRoot, "gradle.properties"),
    ...modulePaths
  ];
}

function newestModifiedTime(pathsToScan) {
  let newest = 0;
  const pending = pathsToScan.filter((candidate) => fs.existsSync(candidate));

  while (pending.length > 0) {
    const current = pending.pop();
    let stat;
    try {
      stat = fs.statSync(current);
    } catch (_error) {
      continue;
    }

    newest = Math.max(newest, stat.mtimeMs);
    if (!stat.isDirectory()) {
      continue;
    }

    let children;
    try {
      children = fs.readdirSync(current, { withFileTypes: true });
    } catch (_error) {
      continue;
    }

    for (const child of children) {
      if (child.name === ".gradle" || child.name === "build") {
        continue;
      }
      pending.push(path.join(current, child.name));
    }
  }

  return newest;
}

function runCommand(command, args, cwd, logKey) {
  return new Promise((resolve) => {
    const child = spawn(command, args, {
      cwd,
      shell: process.platform === "win32",
      env: childEnvironment({ FORCE_COLOR: "1" })
    });

    child.stdout.on("data", (data) => forwardProcessOutput(logKey, data, "info"));
    child.stderr.on("data", (data) => forwardProcessOutput(logKey, data, "warn"));
    child.on("error", (error) => {
      broadcastLog(`${logKey} Fehler: ${error.message}`, logKey, "error");
      resolve({ ok: false, detail: error.message });
    });
    child.on("exit", (code, signal) => {
      if (code === 0) {
        resolve({ ok: true, detail: "ok" });
        return;
      }
      resolve({ ok: false, detail: signal ? `Signal ${signal}` : `Exit ${code}` });
    });
  });
}

function startGameProcess(key, executable, appArgs, startMessage, cwd) {
  if (!fs.existsSync(executable)) {
    const message = `${key === "client" ? "Client" : "Server"}-Starter fehlt: ${executable}`;
    broadcastLog(message);
    return { ok: false, message };
  }

  fs.mkdirSync(cwd, { recursive: true });

  const child = spawn(executable, appArgs, {
    cwd,
    shell: process.platform === "win32",
    env: childEnvironment()
  });

  running.set(key, child);
  broadcastLog(startMessage);

  child.stdout.on("data", (data) => forwardProcessOutput(key, data, "info"));
  child.stderr.on("data", (data) => forwardProcessOutput(key, data, "warn"));
  child.on("error", (error) => {
    running.delete(key);
    broadcastLog(`${key} Fehler: ${error.message}`, key, "error");
  });
  child.on("exit", (code, signal) => {
    running.delete(key);
    broadcastLog(`${key} beendet (${code ?? signal ?? "signal"}).`);
  });

  return { ok: true };
}

function devExecutable(name) {
  const executable = process.platform === "win32" ? `${name}.bat` : name;
  return path.join(devDistributionRoot(name), "bin", executable);
}

function devDistributionRoot(name) {
  return path.join(devProjectRoot, name, "build", "install", name);
}

function bundledExecutable(name) {
  const executable = process.platform === "win32" ? `${name}.bat` : name;
  return path.join(bundledGameRoot, name, "bin", executable);
}

function launcherWorkspaceRoot() {
  return app.isPackaged ? bundledGameRoot : devProjectRoot;
}

function ensureJavaRuntime() {
  const java = javaRuntimeInfo();
  if (java.ok) {
    return null;
  }
  broadcastLog(java.message);
  return { ok: false, message: java.message };
}

function javaRuntimeInfo() {
  const result = spawnSync("java", ["-version"], {
    encoding: "utf8",
    timeout: 5000
  });
  if (result.error) {
    return {
      ok: false,
      version: "",
      message: "Java 21 wurde nicht gefunden. Bitte JDK 21 installieren und java in PATH setzen."
    };
  }
  const output = `${result.stderr || ""}\n${result.stdout || ""}`.trim();
  const firstLine = output.split(/\r?\n/).find(Boolean) || "java -version";
  const major = parseJavaMajorVersion(firstLine);
  if (major < 21) {
    return {
      ok: false,
      version: firstLine,
      message: `Java 21+ wird benoetigt, gefunden wurde: ${firstLine}`
    };
  }
  return {
    ok: true,
    version: firstLine,
    message: firstLine
  };
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

function gradleCommand() {
  return process.platform === "win32" ? "gradlew.bat" : "./gradlew";
}

function childEnvironment(extra = {}) {
  const env = { ...process.env, ...extra };
  delete env.ELECTRON_RENDERER_URL;
  delete env.ELECTRON_RUN_AS_NODE;
  return env;
}

function forwardProcessOutput(key, data, fallbackLevel = "info") {
  data.toString("utf8")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .slice(-6)
    .forEach((line) => {
      const clean = stripAnsi(line);
      broadcastLog(clean, key, inferLogLevel(clean, fallbackLevel));
    });
}

function stripAnsi(line) {
  return line.replace(/\u001b\[[0-9;]*m/g, "");
}

function inferLogLevel(message, fallbackLevel) {
  return /(\berror\b|\bexception\b|failed|fehler|crash)/i.test(message) ? "error" : fallbackLevel;
}

function broadcastLog(message, source = "launcher", level = "info") {
  const entry = {
    time: new Date().toLocaleTimeString("de-DE", { hour: "2-digit", minute: "2-digit", second: "2-digit" }),
    source,
    level,
    message
  };
  BrowserWindow.getAllWindows().forEach((window) => {
    window.webContents.send("launcher:log", entry);
  });
}

function stopAllProcesses() {
  for (const child of running.values()) {
    if (child.exitCode === null) {
      child.kill();
    }
  }
  running.clear();
}

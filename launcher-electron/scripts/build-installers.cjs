const { spawnSync } = require("node:child_process");
const path = require("node:path");

const launcherRoot = path.resolve(__dirname, "..");
const electronBuilderCli = path.join(launcherRoot, "node_modules", "electron-builder", "cli.js");
const requested = process.argv.slice(2);
const targets = expandTargets(requested.length > 0 ? requested : ["current"]);
const matrixMode = targets.length > 1;
const forceCrossBuild = process.env.ADVENTURA_CROSS_BUILD === "1";

runNpm("build:renderer");
runNpm("prepare:game");

for (const target of targets) {
  if (!shouldBuildTarget(target)) {
    continue;
  }
  warnIfCrossPlatform(target);
  runElectronBuilder(target);
}

function expandTargets(values) {
  const expanded = values.flatMap((value) => {
    switch (value) {
      case "all":
        return ["win", "mac", "linux"];
      case "current":
        return [currentTarget()];
      default:
        return [value];
    }
  });
  return [...new Set(expanded)].filter((target) => {
    if (!["win", "mac", "linux"].includes(target)) {
      throw new Error(`Unknown setup target "${target}". Use win, mac, linux, current, or all.`);
    }
    return true;
  });
}

function currentTarget() {
  if (process.platform === "win32") {
    return "win";
  }
  if (process.platform === "darwin") {
    return "mac";
  }
  return "linux";
}

function runNpm(script) {
  const npm = process.platform === "win32" ? "npm.cmd" : "npm";
  run(npm, ["run", script], `npm run ${script}`);
}

function runElectronBuilder(target) {
  const argsByTarget = {
    win: ["--win", "nsis"],
    mac: ["--mac", "dmg", "zip"],
    linux: ["--linux", "AppImage", "deb", "tar.gz"]
  };
  run(process.execPath, [electronBuilderCli, ...argsByTarget[target]], `electron-builder ${target}`);
}

function shouldBuildTarget(target) {
  const host = currentTarget();
  if (target === host || !matrixMode || forceCrossBuild) {
    return true;
  }
  console.warn(`Skipping ${target} setup on ${host}. Run npm run build:setup:${target} on ${target}, or set ADVENTURA_CROSS_BUILD=1 to force a cross-build.`);
  return false;
}

function warnIfCrossPlatform(target) {
  const host = currentTarget();
  if (target === host) {
    return;
  }
  if (target === "mac") {
    console.warn("Note: macOS DMG/ZIP builds are best produced on macOS CI or a macOS machine for signing/notarization.");
    return;
  }
  console.warn(`Note: building ${target} installers from ${host} may require extra host tools in CI.`);
}

function run(command, args, label) {
  console.log(`\n> ${label}`);
  const spawnCommand = process.platform === "win32" && /\.(bat|cmd)$/i.test(command) ? "cmd.exe" : command;
  const spawnArgs = spawnCommand === "cmd.exe" ? ["/d", "/c", "call", command, ...args] : args;
  const result = spawnSync(spawnCommand, spawnArgs, {
    cwd: launcherRoot,
    env: process.env,
    stdio: "inherit",
    windowsHide: true
  });
  if (result.error) {
    console.error(`${label} failed to start: ${result.error.message}`);
    process.exit(1);
  }
  if (result.status !== 0) {
    process.exit(result.status ?? 1);
  }
}

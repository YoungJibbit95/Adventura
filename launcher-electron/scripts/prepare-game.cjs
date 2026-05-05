const { spawnSync } = require("node:child_process");
const fs = require("node:fs");
const path = require("node:path");
const { environmentForRuntime, gradleCommand, runtimeInfo } = require("../electron/platform.cjs");

const launcherRoot = path.resolve(__dirname, "..");
const projectRoot = path.resolve(launcherRoot, "..");
const gradle = gradleCommand(projectRoot);
const gradleArgs = [":client:installDist", ":server:installDist"];
const runtime = runtimeInfo({ projectRoot });
const expectedOutputs = [
  path.join(projectRoot, "client", "build", "install", "client"),
  path.join(projectRoot, "server", "build", "install", "server")
];

if (!runtime.ok) {
  console.error(runtime.message);
  process.exit(1);
}

const result = process.platform === "win32"
  ? spawnSync("cmd.exe", ["/d", "/c", "call", gradle, ...gradleArgs], spawnOptions())
  : spawnSync(gradle, gradleArgs, spawnOptions());

function spawnOptions() {
  return {
    cwd: projectRoot,
    env: environmentForRuntime(runtime, process.env),
    stdio: "inherit",
    windowsHide: true
  };
}

if (result.error) {
  console.error(`Game distribution build could not start: ${result.error.message}`);
  process.exit(1);
}

if (result.status !== 0) {
  process.exit(result.status ?? 1);
}

const missing = expectedOutputs.filter((output) => !fs.existsSync(output));
if (missing.length > 0) {
  console.error(`Missing game distribution output:\n${missing.join("\n")}`);
  process.exit(1);
}

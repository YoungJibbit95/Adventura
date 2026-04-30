const { spawnSync } = require("node:child_process");
const fs = require("node:fs");
const path = require("node:path");

const launcherRoot = path.resolve(__dirname, "..");
const projectRoot = path.resolve(launcherRoot, "..");
const gradle = process.platform === "win32" ? "gradlew.bat" : "./gradlew";
const expectedOutputs = [
  path.join(projectRoot, "client", "build", "install", "client"),
  path.join(projectRoot, "server", "build", "install", "server")
];

const result = spawnSync(gradle, [":client:installDist", ":server:installDist"], {
  cwd: projectRoot,
  stdio: "inherit",
  shell: process.platform === "win32"
});

if (result.status !== 0) {
  process.exit(result.status ?? 1);
}

const missing = expectedOutputs.filter((output) => !fs.existsSync(output));
if (missing.length > 0) {
  console.error(`Missing game distribution output:\n${missing.join("\n")}`);
  process.exit(1);
}

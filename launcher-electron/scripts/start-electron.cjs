const { spawn } = require("node:child_process");
const path = require("node:path");
const electron = require("electron");

const launcherRoot = path.resolve(__dirname, "..");
const env = { ...process.env };

delete env.ELECTRON_RUN_AS_NODE;
delete env.ELECTRON_NO_ATTACH_CONSOLE;

if (process.platform === "linux") {
  env.ELECTRON_DISABLE_SANDBOX = "1";
}

const child = spawn(electron, ["."], {
  cwd: launcherRoot,
  env,
  stdio: "inherit"
});

child.on("error", (error) => {
  console.error(error.message);
  process.exit(1);
});

child.on("exit", (code, signal) => {
  if (signal) {
    process.kill(process.pid, signal);
    return;
  }
  process.exit(code ?? 0);
});

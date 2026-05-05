const { spawn } = require("node:child_process");
const net = require("node:net");
const path = require("node:path");

const launcherRoot = path.resolve(__dirname, "..");
const host = "127.0.0.1";
const preferredPort = Number.parseInt(process.env.ADVENTURA_LAUNCHER_PORT || "5173", 10);
const viteCli = path.join(
  launcherRoot,
  "node_modules",
  "vite",
  "bin",
  "vite.js"
);
let viteProcess = null;
let electronProcess = null;
let shuttingDown = false;

main().catch((error) => {
  console.error(error.message);
  shutdown(1);
});

async function main() {
  const port = await findOpenPort(preferredPort);
  const rendererUrl = `http://${host}:${port}`;

  viteProcess = spawn(process.execPath, [viteCli, "--host", host, "--port", String(port), "--strictPort"], {
    cwd: launcherRoot,
    env: cleanElectronEnv({ ...process.env }),
    stdio: "inherit"
  });

  viteProcess.on("exit", (code, signal) => {
    if (!shuttingDown) {
      shutdown(signal ? 1 : code ?? 0);
    }
  });

  await waitForPort(port);

  electronProcess = spawn(process.execPath, [path.join("scripts", "start-electron.cjs")], {
    cwd: launcherRoot,
    env: cleanElectronEnv({ ...process.env, ELECTRON_RENDERER_URL: rendererUrl }),
    stdio: "inherit"
  });

  electronProcess.on("exit", (code, signal) => {
    shutdown(signal ? 1 : code ?? 0);
  });
}

function findOpenPort(startPort) {
  return new Promise((resolve, reject) => {
    const tryPort = (port) => {
      if (port > startPort + 40) {
        reject(new Error(`Kein freier Launcher-Port ab ${startPort} gefunden.`));
        return;
      }

      const server = net.createServer();
      server.once("error", () => tryPort(port + 1));
      server.once("listening", () => {
        server.close(() => resolve(port));
      });
      server.listen(port, host);
    };

    tryPort(startPort);
  });
}

function waitForPort(port) {
  return new Promise((resolve, reject) => {
    const deadline = Date.now() + 15000;
    const tryConnect = () => {
      const socket = net.createConnection({ host, port });
      socket.once("connect", () => {
        socket.destroy();
        resolve();
      });
      socket.once("error", () => {
        socket.destroy();
        if (Date.now() > deadline) {
          reject(new Error(`Vite ist auf Port ${port} nicht rechtzeitig gestartet.`));
          return;
        }
        setTimeout(tryConnect, 120);
      });
    };

    tryConnect();
  });
}

function cleanElectronEnv(env) {
  delete env.ELECTRON_RUN_AS_NODE;
  delete env.ELECTRON_NO_ATTACH_CONSOLE;
  return env;
}

function shutdown(code) {
  if (shuttingDown) {
    return;
  }
  shuttingDown = true;

  for (const child of [electronProcess, viteProcess]) {
    if (child && child.exitCode === null) {
      child.kill();
    }
  }

  process.exit(code);
}

process.on("SIGINT", () => shutdown(130));
process.on("SIGTERM", () => shutdown(143));

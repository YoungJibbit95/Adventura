const { app, BrowserWindow, ipcMain, shell } = require("electron");
const { spawn } = require("node:child_process");
const fs = require("node:fs");
const fsp = require("node:fs/promises");
const net = require("node:net");
const os = require("node:os");
const path = require("node:path");
const {
  environmentForRuntime,
  gameExecutable,
  gradleCommand: platformGradleCommand,
  platformKey,
  runtimeInfo
} = require("./platform.cjs");

const devProjectRoot = path.resolve(__dirname, "..", "..");
const bundledGameRoot = path.join(process.resourcesPath, "game");
const settingsPath = path.join(os.homedir(), ".adventura", "launcher.properties");
const packagedRuntimeRoot = path.join(os.homedir(), ".adventura", "runtime");
const distributionFreshnessTtlMs = 2500;
const startConfirmationMs = 1800;
const serverReadyTimeoutMs = 14000;
const running = new Map();
const distributionFreshnessCache = new Map();
let resolvedJavaRuntime = null;
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
  app.commandLine.appendSwitch("disable-gpu-sandbox");
}

function createWindow() {
  const win = new BrowserWindow({
    title: "Adventura Launcher",
    width: 1180,
    height: 760,
    minWidth: 860,
    minHeight: 600,
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
  platform: platformKey(),
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

ipcMain.handle("launcher:getStatus", () => processSnapshot());

ipcMain.handle("launcher:launch", async (_event, payload) => {
  const settings = sanitizeSettings(payload?.settings || defaults);
  await saveSettings(settings);

  if (payload?.mode === "game") {
    return startClient("Adventura", mainMenuArgs(settings));
  }

  if (payload?.mode === "server") {
    return startServer(settings);
  }

  if (payload?.mode === "multiplayer" && settings.autoStartServer) {
    const serverResult = await startServer(settings);
    if (!serverResult.ok && !serverResult.alreadyRunning) {
      return serverResult;
    }
    const joinSettings = localJoinSettings(settings);
    const ready = await waitForServerReady(joinSettings.host, joinSettings.port);
    if (!ready.ok) {
      return ready;
    }
    return startClient("Multiplayer", clientArgs(joinSettings, true));
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
    broadcastStatus();
    return { ok: true };
  }
  const child = running.get(target);
  if (!child) {
    return { ok: false, message: "Kein laufender Prozess gefunden." };
  }
  killProcessTree(child);
  running.delete(target);
  broadcastLog(`${target} wurde gestoppt.`);
  broadcastStatus();
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

function mainMenuArgs(settings) {
  return [
    "--seed", settings.seed,
    "--username", settings.username,
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
    return startGameProcess("server", bundledExecutable("server"), args, `Server startet auf Port ${settings.port}.`, packagedRuntimeRoot);
  }
  const prepared = await ensureDevDistribution("server");
  if (!prepared.ok) {
    return prepared;
  }
  return startGameProcess("server", devExecutable("server"), args, `Server startet auf Port ${settings.port}.`, devProjectRoot);
}

function preflight(inputSettings) {
  const settings = sanitizeSettings(inputSettings);
  const java = javaRuntimeInfo();
  const checks = [
    {
      label: "Platform",
      status: "ok",
      detail: `${platformKey()} / ${app.isPackaged ? "packaged" : "workspace"}`
    },
    {
      label: "Java 21",
      status: java.ok ? "ok" : "error",
      detail: java.ok ? `${java.version} (${java.source})` : java.message
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
  const wrapper = path.join(devProjectRoot, process.platform === "win32" ? "gradlew.bat" : "gradlew");
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
  distributionFreshnessCache.delete(name);

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
  const cached = distributionFreshnessCache.get(name);
  if (cached && Date.now() - cached.time < distributionFreshnessTtlMs) {
    return cached.fresh;
  }

  const outputTime = newestModifiedTime([devDistributionRoot(name)]);
  const inputTime = newestModifiedTime(devInputPaths(name));
  const fresh = outputTime > 0 && outputTime >= inputTime;
  distributionFreshnessCache.set(name, { time: Date.now(), fresh });
  return fresh;
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
    const child = spawnLauncherCommand(command, args, {
      cwd,
      env: childEnvironment({ FORCE_COLOR: "1" }),
      windowsHide: true
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

  const child = spawnLauncherCommand(executable, appArgs, {
    cwd,
    env: childEnvironment(),
    windowsHide: true
  });

  running.set(key, child);
  broadcastLog(startMessage);
  broadcastStatus();

  return new Promise((resolve) => {
    let settled = false;
    const confirmTimer = setTimeout(() => {
      if (!settled) {
        settled = true;
        resolve({ ok: true, pid: child.pid });
      }
    }, startConfirmationMs);

    child.stdout.on("data", (data) => forwardProcessOutput(key, data, "info"));
    child.stderr.on("data", (data) => forwardProcessOutput(key, data, "warn"));
    child.on("error", (error) => {
      clearTimeout(confirmTimer);
      running.delete(key);
      broadcastLog(`${key} Fehler: ${error.message}`, key, "error");
      broadcastStatus();
      if (!settled) {
        settled = true;
        resolve({ ok: false, message: error.message });
      }
    });
    child.on("exit", (code, signal) => {
      clearTimeout(confirmTimer);
      running.delete(key);
      const detail = signal ? `Signal ${signal}` : `Exit ${code ?? "unbekannt"}`;
      broadcastLog(`${key} beendet (${detail}).`, key, code === 0 ? "info" : "warn");
      broadcastStatus();
      if (!settled) {
        settled = true;
        resolve({ ok: false, message: `${key} konnte nicht stabil starten (${detail}).` });
      }
    });
  });
}

function devExecutable(name) {
  return gameExecutable(path.join(devProjectRoot, name, "build", "install"), name);
}

function devDistributionRoot(name) {
  return path.join(devProjectRoot, name, "build", "install", name);
}

function bundledExecutable(name) {
  return gameExecutable(bundledGameRoot, name);
}

function launcherWorkspaceRoot() {
  return app.isPackaged ? bundledGameRoot : devProjectRoot;
}

function ensureJavaRuntime() {
  const java = javaRuntimeInfo();
  if (java.ok) {
    resolvedJavaRuntime = java;
    return null;
  }
  resolvedJavaRuntime = null;
  broadcastLog(java.message);
  return { ok: false, message: java.message };
}

function localJoinSettings(settings) {
  const host = localConnectHost(settings.host);
  if (host !== settings.host) {
    broadcastLog(`Auto Server verbindet lokal ueber ${host}:${settings.port} statt ${settings.host}:${settings.port}.`);
  }
  return { ...settings, host };
}

function localConnectHost(host) {
  const normalized = String(host || "").trim().toLowerCase();
  if (!normalized || normalized === "0.0.0.0" || normalized === "::") {
    return "127.0.0.1";
  }
  if (["127.0.0.1", "localhost", "::1"].includes(normalized)) {
    return host;
  }
  return "127.0.0.1";
}

function waitForServerReady(host, port) {
  broadcastLog(`Warte auf Server ${host}:${port}.`, "server");
  const deadline = Date.now() + serverReadyTimeoutMs;

  return new Promise((resolve) => {
    const tryConnect = () => {
      const socket = net.createConnection({ host, port });
      let settled = false;
      const finish = (ok, detail) => {
        if (settled) {
          return;
        }
        settled = true;
        socket.destroy();
        if (ok) {
          broadcastLog(`Server ist erreichbar auf ${host}:${port}.`, "server", "ok");
          resolve({ ok: true });
          return;
        }
        if (Date.now() >= deadline) {
          const message = `Server wurde nicht rechtzeitig erreichbar (${detail}).`;
          broadcastLog(message, "server", "error");
          resolve({ ok: false, message });
          return;
        }
        setTimeout(tryConnect, 240);
      };

      socket.setTimeout(900);
      socket.once("connect", () => finish(true, "ok"));
      socket.once("timeout", () => finish(false, "Timeout"));
      socket.once("error", (error) => finish(false, error.code || error.message));
    };

    tryConnect();
  });
}

function javaRuntimeInfo() {
  return runtimeInfo({
    resourcesPath: process.resourcesPath,
    projectRoot: devProjectRoot
  });
}

function gradleCommand() {
  return platformGradleCommand(devProjectRoot);
}

function childEnvironment(extra = {}) {
  const base = resolvedJavaRuntime?.ok
    ? environmentForRuntime(resolvedJavaRuntime, process.env)
    : { ...process.env };
  const env = { ...base, ...extra };
  delete env.ELECTRON_RENDERER_URL;
  delete env.ELECTRON_RUN_AS_NODE;
  return env;
}

function spawnLauncherCommand(command, args, options) {
  if (process.platform === "win32" && /\.(bat|cmd)$/i.test(command)) {
    return spawn("cmd.exe", ["/d", "/c", "call", command, ...args], options);
  }
  return spawn(command, args, options);
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

function processSnapshot() {
  return {
    client: processState("client"),
    server: processState("server")
  };
}

function processState(key) {
  const child = running.get(key);
  const active = Boolean(child && child.exitCode === null);
  return {
    running: active,
    pid: active ? child.pid : null
  };
}

function broadcastStatus() {
  const status = processSnapshot();
  BrowserWindow.getAllWindows().forEach((window) => {
    window.webContents.send("launcher:status", status);
  });
}

function stopAllProcesses() {
  for (const child of running.values()) {
    if (child.exitCode === null) {
      killProcessTree(child);
    }
  }
  running.clear();
}

function killProcessTree(child) {
  if (!child || child.exitCode !== null) {
    return;
  }
  if (process.platform === "win32" && child.pid) {
    spawn("taskkill", ["/pid", String(child.pid), "/t", "/f"], {
      windowsHide: true,
      stdio: "ignore"
    });
    return;
  }
  child.kill();
}

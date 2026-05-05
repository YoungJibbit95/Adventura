import { useEffect, useMemo, useRef, useState } from "react";
import { defaultSettings, modeLabels } from "../data/launcherData";
import { adventuraApi } from "../lib/adventuraApi";

const logLimit = 180;
const initialProcessStatus = {
  client: { running: false, pid: null },
  server: { running: false, pid: null }
};

const initialLog = {
  time: new Date().toLocaleTimeString("de-DE"),
  source: "launcher",
  level: "info",
  message: "Launcher bereit."
};

export function useLauncherState() {
  const [activeTab, setActiveTab] = useState("play");
  const [settings, setSettings] = useState(defaultSettings);
  const [runtime, setRuntime] = useState(null);
  const [logs, setLogs] = useState([initialLog]);
  const [notice, setNotice] = useState("");
  const [busyMode, setBusyMode] = useState("");
  const [preflight, setPreflight] = useState(null);
  const [preflightBusy, setPreflightBusy] = useState(false);
  const [processStatus, setProcessStatus] = useState(initialProcessStatus);
  const noticeTimerRef = useRef(0);
  const busyTimerRef = useRef(0);
  const preflightRequestRef = useRef(0);

  useEffect(() => {
    let logCleanup = () => {};
    let statusCleanup = () => {};
    let mounted = true;

    adventuraApi.getRuntimeInfo().then((info) => {
      if (mounted) {
        setRuntime(info);
      }
    }).catch(() => mounted && setRuntime(null));
    adventuraApi.loadSettings()
      .then((loadedSettings) => {
        if (!mounted) {
          return;
        }
        setSettings(loadedSettings);
        runPreflight(loadedSettings, false);
      })
      .catch(() => {
        if (!mounted) {
          return;
        }
        setSettings(defaultSettings);
        runPreflight(defaultSettings, false);
      });
    adventuraApi.getStatus()
      .then((status) => mounted && setProcessStatus(status))
      .catch(() => mounted && setProcessStatus(initialProcessStatus));
    logCleanup = adventuraApi.onLog((entry) => {
      setLogs((current) => {
        const retained = current.length >= logLimit ? current.slice(-(logLimit - 1)) : current;
        return [...retained, entry];
      });
    });
    statusCleanup = adventuraApi.onStatus((status) => {
      setProcessStatus(status);
    });

    return () => {
      mounted = false;
      logCleanup();
      statusCleanup();
      window.clearTimeout(noticeTimerRef.current);
      window.clearTimeout(busyTimerRef.current);
    };
  }, []);

  const summary = useMemo(() => ({
    profile: settings.username || "Player",
    world: `Seed ${settings.seed || "1337"}`,
    render: `${settings.renderDistance} Chunks`,
    network: `${settings.host}:${settings.port}`
  }), [settings]);

  function updateField(key, value) {
    setSettings((current) => ({ ...current, [key]: value }));
  }

  function updateNumber(key, value) {
    updateField(key, Number.parseInt(value, 10));
  }

  async function save() {
    try {
      const saved = await adventuraApi.saveSettings(settings);
      setSettings(saved);
      showNotice("Gespeichert");
    } catch (error) {
      showNotice(`Fehler: ${error.message}`);
    }
  }

  async function launch(mode) {
    setBusyMode(mode);
    showNotice(`${modeLabels[mode]} wird vorbereitet...`, 2600);
    try {
      const saved = await adventuraApi.saveSettings(settings);
      setSettings(saved);
      const result = await adventuraApi.launch({ mode, settings: saved });
      showNotice(result.ok ? `${modeLabels[mode]} startet` : result.message || "Start fehlgeschlagen", 2600);
      if (!result.ok && !result.alreadyRunning) {
        setActiveTab("terminal");
      }
      runPreflight(saved, false);
    } catch (error) {
      showNotice(`Startfehler: ${error.message}`, 3000);
      setActiveTab("terminal");
    } finally {
      window.clearTimeout(busyTimerRef.current);
      busyTimerRef.current = window.setTimeout(() => setBusyMode(""), 700);
    }
  }

  async function stopAll() {
    try {
      await adventuraApi.stop("all");
      showNotice("Prozesse gestoppt");
    } catch (error) {
      showNotice(`Stopfehler: ${error.message}`);
    }
  }

  function resetSettings() {
    setSettings(defaultSettings);
    showNotice("Standardwerte geladen");
  }

  function applyPreset(preset) {
    setSettings((current) => ({
      ...current,
      renderDistance: preset.renderDistance,
      previewRadius: preset.previewRadius
    }));
  }

  async function runPreflight(nextSettings = settings, showTab = true) {
    const requestId = preflightRequestRef.current + 1;
    preflightRequestRef.current = requestId;
    setPreflightBusy(true);
    if (showTab) {
      setActiveTab("terminal");
    }
    try {
      const result = await adventuraApi.preflight({ settings: nextSettings });
      if (requestId === preflightRequestRef.current) {
        setPreflight(result);
      }
      if (showTab) {
        showNotice(result.ok ? "Preflight ok" : "Preflight hat Fehler", 2200);
      }
    } catch (error) {
      if (requestId === preflightRequestRef.current) {
        setPreflight({
          ok: false,
          checks: [
            { label: "Preflight", status: "error", detail: error.message }
          ]
        });
      }
      showNotice(`Preflight Fehler: ${error.message}`, 2600);
    } finally {
      if (requestId === preflightRequestRef.current) {
        setPreflightBusy(false);
      }
    }
  }

  function showNotice(message, duration = 1800) {
    window.clearTimeout(noticeTimerRef.current);
    setNotice(message);
    noticeTimerRef.current = window.setTimeout(() => setNotice(""), duration);
  }

  return {
    activeTab,
    setActiveTab,
    settings,
    runtime,
    logs,
    notice,
    busyMode,
    summary,
    preflight,
    preflightBusy,
    processStatus,
    updateField,
    updateNumber,
    save,
    launch,
    stopAll,
    resetSettings,
    applyPreset,
    runPreflight,
    openWorkspace: adventuraApi.openWorkspace
  };
}

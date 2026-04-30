import { useEffect, useMemo, useState } from "react";
import { defaultSettings, modeLabels } from "../data/launcherData";
import { adventuraApi } from "../lib/adventuraApi";

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

  useEffect(() => {
    let cleanup = () => {};
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
    cleanup = adventuraApi.onLog((entry) => {
      setLogs((current) => [...current.slice(-140), entry]);
    });

    return () => {
      mounted = false;
      cleanup();
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
      showNotice(result.ok ? `${modeLabels[mode]} startet` : result.message, 2600);
      runPreflight(saved, false);
    } catch (error) {
      showNotice(`Startfehler: ${error.message}`, 3000);
      setActiveTab("terminal");
    } finally {
      window.setTimeout(() => setBusyMode(""), 700);
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
    setPreflightBusy(true);
    if (showTab) {
      setActiveTab("terminal");
    }
    try {
      const result = await adventuraApi.preflight({ settings: nextSettings });
      setPreflight(result);
      if (showTab) {
        showNotice(result.ok ? "Preflight ok" : "Preflight hat Fehler", 2200);
      }
    } catch (error) {
      setPreflight({
        ok: false,
        checks: [
          { label: "Preflight", status: "error", detail: error.message }
        ]
      });
      showNotice(`Preflight Fehler: ${error.message}`, 2600);
    } finally {
      setPreflightBusy(false);
    }
  }

  function showNotice(message, duration = 1800) {
    setNotice(message);
    window.setTimeout(() => setNotice(""), duration);
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

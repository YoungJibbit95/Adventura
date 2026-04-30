import { defaultSettings } from "../data/launcherData";

export const adventuraApi = window.adventura ?? {
  getRuntimeInfo: async () => ({
    projectRoot: "Browser preview",
    launchMode: "preview",
    java: { ok: true, version: "Browser preview" },
    settingsPath: "~/.adventura/launcher.properties",
    gradle: "./gradlew",
    packaged: false
  }),
  loadSettings: async () => {
    const saved = window.localStorage.getItem("adventura.launcher.preview");
    return saved ? JSON.parse(saved) : defaultSettings;
  },
  saveSettings: async (settings) => {
    window.localStorage.setItem("adventura.launcher.preview", JSON.stringify(settings));
    return settings;
  },
  launch: async () => ({ ok: true }),
  preflight: async () => ({
    ok: true,
    checks: [
      { label: "Preview", status: "ok", detail: "Browser preview kann keinen Client starten." }
    ]
  }),
  stop: async () => ({ ok: true }),
  openWorkspace: async () => ({ ok: true }),
  onLog: () => () => {}
};

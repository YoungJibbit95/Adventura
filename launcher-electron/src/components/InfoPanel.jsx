import { Activity, Info, Square } from "lucide-react";
import { InfoRow, PanelTitle } from "./common";

export function InfoPanel({ logs, runtime, settings, onStopAll }) {
  return (
    <section className="info-layout">
      <div className="runtime-panel">
        <PanelTitle title="Runtime" icon={Activity} />
        <InfoRow label="Launch Mode" value={runtimeLabel(runtime?.launchMode)} />
        <InfoRow label="Platform" value={runtime?.platform || "..."} />
        <InfoRow label="Java" value={runtime?.java?.version || runtime?.java?.message || "..."} />
        <InfoRow label="Workspace" value={runtime?.projectRoot || "..."} />
        <InfoRow label="Settings" value={runtime?.settingsPath || "..."} />
        <InfoRow label="Gradle" value={runtime?.gradle || "./gradlew"} />
        <InfoRow label="Profil" value={`${settings.username} / ${settings.host}:${settings.port}`} />
        <button className="danger-action" type="button" onClick={onStopAll}>
          <Square size={17} />
          <span>Alle Prozesse stoppen</span>
        </button>
      </div>

      <div className="log-panel">
        <PanelTitle title="Launcher Log" icon={Info} />
        <div className="log-list">
          {logs.map((entry, index) => (
            <div className="log-line" key={`${entry.time}-${index}`}>
              <time>{entry.time}</time>
              <span>{entry.message}</span>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function runtimeLabel(mode) {
  if (mode === "bundled") {
    return "Bundled desktop build";
  }
  if (mode === "preview") {
    return "Browser preview";
  }
  return "InstallDist workspace";
}

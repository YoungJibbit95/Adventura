import { Gauge, LoaderCircle, Server, Shield, Sparkles, User } from "lucide-react";
import { launchActions, modeLabels, questCards } from "../data/launcherData";
import { Metric } from "./common";
import { PixelWorld } from "./PixelWorld";

export function PlayPanel({ summary, settings, busyMode, onLaunch }) {
  const launching = Boolean(busyMode);
  const busyLabel = modeLabels[busyMode] || "Spiel";

  return (
    <section className="content-grid play-grid">
      <div className="launch-stage">
        <PixelWorld />
        <div className="launch-copy">
          <span className="eyebrow amber">Bereit</span>
          <h3>Welt laden, Server joinen oder lokal testen.</h3>
        </div>
        <div className={launching ? "launch-status active" : "launch-status"}>
          <span className={launching ? "state-dot on" : "state-dot"} />
          <div>
            <strong>{launching ? `${busyLabel} wird vorbereitet` : "Startklar"}</strong>
            <span>{launching ? "Build wird geprüft, danach startet der Client." : "Settings sind bereit und werden beim Start gespeichert."}</span>
          </div>
          <Sparkles size={18} />
        </div>
        <div className="launch-actions">
          {launchActions.map((action) => {
            const Icon = busyMode === action.mode ? LoaderCircle : action.icon;
            return (
              <button
                key={action.mode}
                className={`${action.className}${busyMode === action.mode ? " busy" : ""}`}
                type="button"
                disabled={launching}
                onClick={() => onLaunch(action.mode)}
              >
                <Icon className={busyMode === action.mode ? "spin-icon" : ""} size={20} />
                <span>{busyMode === action.mode ? "Startet..." : action.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      <div className="overview-panel">
        <Metric icon={User} label="Profil" value={summary.profile} tone="green" />
        <Metric icon={Shield} label="Welt" value={summary.world} tone="amber" />
        <Metric icon={Gauge} label="Render" value={summary.render} tone="cyan" />
        <Metric icon={Server} label="Server" value={summary.network} tone="violet" />
        <div className="auto-server">
          <span className={settings.autoStartServer ? "state-dot on" : "state-dot"} />
          <div>
            <strong>Auto Server</strong>
            <span>{settings.autoStartServer ? "Aktiv fuer Multiplayer" : "Manuell"}</span>
          </div>
        </div>
        <div className="quest-stack">
          {questCards.map((quest) => {
            const Icon = quest.icon;
            return (
              <div className="quest-card" key={quest.title}>
                <Icon size={17} />
                <div>
                  <strong>{quest.title}</strong>
                  <span>{quest.text}</span>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}

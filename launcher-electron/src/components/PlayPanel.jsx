import { Gauge, LoaderCircle, Server, Shield, Sparkles, User } from "lucide-react";
import { launchActions, modeLabels, questCards } from "../data/launcherData";
import { Metric } from "./common";
import { PixelWorld } from "./PixelWorld";

export function PlayPanel({ summary, settings, busyMode, processStatus, preflight, onLaunch }) {
  const launching = Boolean(busyMode);
  const busyLabel = modeLabels[busyMode] || "Spiel";
  const clientRunning = Boolean(processStatus?.client?.running);
  const serverRunning = Boolean(processStatus?.server?.running);
  const mainAction = launchActions.find((action) => action.mode === "game") || launchActions[0];
  const quickActions = launchActions.filter((action) => action.mode !== mainAction.mode);
  const MainIcon = busyMode === mainAction.mode ? LoaderCircle : mainAction.icon;
  const statusTitle = statusHeadline(launching, busyLabel, clientRunning, serverRunning, preflight);
  const statusDetail = statusText(launching, clientRunning, serverRunning, preflight);

  return (
    <section className="content-grid play-grid">
      <div className="launch-stage">
        <PixelWorld />

        <div className="stage-overlay">
          <div className="stage-title">
            <span className="eyebrow amber">Adventura</span>
            <h3>Hauptmenue starten</h3>
          </div>

          <button
            className={`${mainAction.className}${busyMode === mainAction.mode ? " busy" : ""}`}
            type="button"
            disabled={launching || clientRunning}
            onClick={() => onLaunch(mainAction.mode)}
          >
            <MainIcon className={busyMode === mainAction.mode ? "spin-icon" : ""} size={22} />
            <span>{buttonLabel(mainAction, busyMode, clientRunning)}</span>
          </button>
        </div>

        <div className={launching || clientRunning || serverRunning ? "launch-status active" : "launch-status"}>
          <span className={launching || clientRunning || serverRunning ? "state-dot on" : "state-dot"} />
          <div>
            <strong>{statusTitle}</strong>
            <span>{statusDetail}</span>
          </div>
          <Sparkles size={18} />
        </div>

        <div className="process-strip">
          <ProcessPill label="Client" state={processStatus?.client} />
          <ProcessPill label="Server" state={processStatus?.server} />
        </div>

        <div className="quick-launch">
          {quickActions.map((action) => {
            const Icon = busyMode === action.mode ? LoaderCircle : action.icon;
            const running = action.mode === "server" ? serverRunning : clientRunning;
            return (
              <button
                key={action.mode}
                className={`${action.className}${busyMode === action.mode ? " busy" : ""}`}
                type="button"
                disabled={launching || running}
                onClick={() => onLaunch(action.mode)}
              >
                <Icon className={busyMode === action.mode ? "spin-icon" : ""} size={18} />
                <span>{buttonLabel(action, busyMode, running)}</span>
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
            <span>{settings.autoStartServer ? "Aktiv" : "Aus"}</span>
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

function statusHeadline(launching, busyLabel, clientRunning, serverRunning, preflight) {
  if (launching) {
    return `${busyLabel} wird vorbereitet`;
  }
  if (clientRunning) {
    return "Spiel laeuft";
  }
  if (serverRunning) {
    return "Server laeuft";
  }
  if (preflight?.ok === false) {
    return "Start Check offen";
  }
  return "Bereit";
}

function statusText(launching, clientRunning, serverRunning, preflight) {
  if (launching) {
    return "Client wird gebaut, geprueft und gestartet.";
  }
  if (clientRunning) {
    return "Adventura ist offen. Weitere Client-Starts sind gesperrt.";
  }
  if (serverRunning) {
    return "Lokaler Server ist aktiv.";
  }
  if (preflight?.ok === false) {
    return "Details stehen im Terminal.";
  }
  return "Spiel oeffnet jetzt im Hauptmenue.";
}

function buttonLabel(action, busyMode, running) {
  if (busyMode === action.mode) {
    return "Startet...";
  }
  if (running) {
    return action.mode === "server" ? "Server laeuft" : "Spiel laeuft";
  }
  return action.label;
}

function ProcessPill({ label, state }) {
  const running = Boolean(state?.running);
  return (
    <div className={running ? "process-pill on" : "process-pill"}>
      <span />
      <strong>{label}</strong>
      <small>{running ? `PID ${state.pid}` : "bereit"}</small>
    </div>
  );
}

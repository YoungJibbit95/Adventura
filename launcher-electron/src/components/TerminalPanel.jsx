import { CheckCircle2, CircleAlert, CircleDashed, Square, Terminal } from "lucide-react";
import { useEffect, useMemo, useRef } from "react";
import { PanelTitle } from "./common";

export function TerminalPanel({ logs, preflight, preflightBusy, onRunPreflight, onStopAll }) {
  const terminalRef = useRef(null);
  const statusCounts = useMemo(() => countStatuses(preflight?.checks || []), [preflight]);

  useEffect(() => {
    const element = terminalRef.current;
    if (element) {
      element.scrollTop = element.scrollHeight;
    }
  }, [logs]);

  return (
    <section className="terminal-layout">
      <div className="terminal-card">
        <div className="terminal-head">
          <PanelTitle title="Client Terminal" icon={Terminal} />
          <div className="terminal-actions">
            <button className="secondary-action" type="button" onClick={onRunPreflight} disabled={preflightBusy}>
              <CircleDashed className={preflightBusy ? "spin-icon" : ""} size={17} />
              <span>{preflightBusy ? "Prueft..." : "Preflight"}</span>
            </button>
            <button className="danger-action compact" type="button" onClick={onStopAll}>
              <Square size={16} />
              <span>Stop</span>
            </button>
          </div>
        </div>

        <div className="terminal-screen" ref={terminalRef}>
          {logs.map((entry, index) => (
            <div className={`terminal-line ${entry.level || "info"}`} key={`${entry.time}-${index}`}>
              <time>{entry.time}</time>
              <strong>{entry.source || "launcher"}</strong>
              <span>{entry.message}</span>
            </div>
          ))}
        </div>
      </div>

      <aside className="preflight-card">
        <PanelTitle title="Start Check" icon={preflight?.ok ? CheckCircle2 : CircleAlert} />
        <div className="preflight-summary">
          <strong>{preflight?.ok ? "Client kann starten" : "Noch nicht startklar"}</strong>
          <span>{statusCounts.ok} ok / {statusCounts.warn} hinweise / {statusCounts.error} fehler</span>
        </div>
        <div className="preflight-list">
          {(preflight?.checks || []).map((check) => (
            <div className={`preflight-row ${check.status}`} key={check.label}>
              <span />
              <div>
                <strong>{check.label}</strong>
                <small>{check.detail}</small>
              </div>
            </div>
          ))}
        </div>
      </aside>
    </section>
  );
}

function countStatuses(checks) {
  return checks.reduce((counts, check) => {
    counts[check.status] = (counts[check.status] || 0) + 1;
    return counts;
  }, { ok: 0, warn: 0, error: 0 });
}

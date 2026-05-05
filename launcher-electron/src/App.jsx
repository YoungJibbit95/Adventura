import { InfoPanel } from "./components/InfoPanel";
import { PlayPanel } from "./components/PlayPanel";
import { SettingsPanel } from "./components/SettingsPanel";
import { Sidebar } from "./components/Sidebar";
import { TerminalPanel } from "./components/TerminalPanel";
import { Topbar } from "./components/Topbar";
import { useLauncherState } from "./hooks/useLauncherState";

export default function App() {
  const launcher = useLauncherState();

  return (
    <div className="app-shell">
      <Sidebar activeTab={launcher.activeTab} runtime={launcher.runtime} onTabChange={launcher.setActiveTab} />

      <main className="main">
        <Topbar activeTab={launcher.activeTab} notice={launcher.notice} onOpenWorkspace={launcher.openWorkspace} />

        {launcher.activeTab === "play" && (
          <PlayPanel
            summary={launcher.summary}
            settings={launcher.settings}
            busyMode={launcher.busyMode}
            processStatus={launcher.processStatus}
            preflight={launcher.preflight}
            onLaunch={launcher.launch}
          />
        )}

        {launcher.activeTab === "settings" && (
          <SettingsPanel
            settings={launcher.settings}
            onChange={launcher.updateField}
            onNumberChange={launcher.updateNumber}
            onSave={launcher.save}
            onReset={launcher.resetSettings}
            onPreset={launcher.applyPreset}
          />
        )}

        {launcher.activeTab === "terminal" && (
          <TerminalPanel
            logs={launcher.logs}
            preflight={launcher.preflight}
            preflightBusy={launcher.preflightBusy}
            onRunPreflight={() => launcher.runPreflight()}
            onStopAll={launcher.stopAll}
          />
        )}

        {launcher.activeTab === "info" && (
          <InfoPanel
            logs={launcher.logs}
            runtime={launcher.runtime}
            settings={launcher.settings}
            onStopAll={launcher.stopAll}
          />
        )}
      </main>
    </div>
  );
}

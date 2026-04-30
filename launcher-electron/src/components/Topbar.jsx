import { FolderOpen } from "lucide-react";

const titles = {
  play: "Spiel starten",
  settings: "Launcher Setup",
  terminal: "Client Terminal",
  info: "Status Center"
};

export function Topbar({ activeTab, notice, onOpenWorkspace }) {
  return (
    <header className="topbar">
      <div>
        <span className="eyebrow">AdventureCraft Style</span>
        <h2>{titles[activeTab] || "Adventura"}</h2>
      </div>
      <div className="top-actions">
        {notice && <span className="notice">{notice}</span>}
        <button className="icon-button" type="button" onClick={onOpenWorkspace} aria-label="Workspace öffnen">
          <FolderOpen size={18} />
        </button>
      </div>
    </header>
  );
}

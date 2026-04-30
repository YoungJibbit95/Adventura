import { tabs } from "../data/launcherData";

export function Sidebar({ activeTab, runtime, onTabChange }) {
  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="brand-mark" aria-hidden="true">
          <span />
        </div>
        <div>
          <h1>Adventura</h1>
          <p>Native Launcher</p>
        </div>
      </div>

      <nav className="nav-tabs">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          return (
            <button
              key={tab.id}
              className={activeTab === tab.id ? "nav-tab active" : "nav-tab"}
              type="button"
              onClick={() => onTabChange(tab.id)}
            >
              <Icon size={18} />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </nav>

      <div className="mini-status">
        <span className="pulse" />
        <div>
          <strong>Workspace</strong>
          <span>{runtime?.packaged ? "Packaged" : "Development"}</span>
        </div>
      </div>
    </aside>
  );
}

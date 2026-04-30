import { Gauge, RotateCcw, Save, Server, User } from "lucide-react";
import { presets } from "../data/launcherData";
import { Field, PanelTitle, RangeField } from "./common";

export function SettingsPanel({ settings, onChange, onNumberChange, onSave, onReset, onPreset }) {
  return (
    <section className="settings-layout">
      <div className="settings-column">
        <PanelTitle title="Profil & Welt" icon={User} />
        <div className="form-grid">
          <Field label="Username">
            <input value={settings.username} maxLength={32} onChange={(event) => onChange("username", event.target.value)} />
          </Field>
          <Field label="Seed">
            <input value={settings.seed} inputMode="numeric" onChange={(event) => onChange("seed", event.target.value)} />
          </Field>
        </div>

        <PanelTitle title="Rendering" icon={Gauge} />
        <RangeField
          label="Render Distance"
          value={settings.renderDistance}
          min={2}
          max={18}
          suffix="Chunks"
          onChange={(value) => onNumberChange("renderDistance", value)}
        />
        <RangeField
          label="Preview Radius"
          value={settings.previewRadius}
          min={1}
          max={8}
          suffix="Chunks"
          onChange={(value) => onNumberChange("previewRadius", value)}
        />
      </div>

      <div className="settings-column">
        <PanelTitle title="Server" icon={Server} />
        <div className="form-grid">
          <Field label="Host">
            <input value={settings.host} maxLength={255} onChange={(event) => onChange("host", event.target.value)} />
          </Field>
          <Field label="Port">
            <input value={settings.port} inputMode="numeric" onChange={(event) => onChange("port", event.target.value)} />
          </Field>
        </div>

        <label className="toggle-row">
          <input
            type="checkbox"
            checked={settings.autoStartServer}
            onChange={(event) => onChange("autoStartServer", event.target.checked)}
          />
          <span />
          <div>
            <strong>Lokalen Server vor Join starten</strong>
            <small>Multiplayer nutzt dann automatisch die lokale Instanz.</small>
          </div>
        </label>

        <div className="preset-row">
          {presets.map((preset) => (
            <button key={preset.name} type="button" onClick={() => onPreset(preset)}>
              {preset.name}
            </button>
          ))}
        </div>

        <div className="settings-actions">
          <button className="secondary-action" type="button" onClick={onReset}>
            <RotateCcw size={18} />
            <span>Reset</span>
          </button>
          <button className="primary-action" type="button" onClick={onSave}>
            <Save size={18} />
            <span>Speichern</span>
          </button>
        </div>
      </div>
    </section>
  );
}

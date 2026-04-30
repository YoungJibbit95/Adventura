import {
  Gauge,
  Info,
  MonitorPlay,
  Play,
  Server,
  Settings,
  Terminal,
  Trophy,
  Wifi
} from "lucide-react";

export const defaultSettings = {
  username: "Player",
  seed: "1337",
  renderDistance: 8,
  previewRadius: 3,
  host: "127.0.0.1",
  port: 25565,
  autoStartServer: false
};

export const tabs = [
  { id: "play", label: "Spielen", icon: Play },
  { id: "settings", label: "Settings", icon: Settings },
  { id: "terminal", label: "Terminal", icon: Terminal },
  { id: "info", label: "Info", icon: Info }
];

export const presets = [
  { name: "Fast", renderDistance: 6, previewRadius: 2 },
  { name: "Balanced", renderDistance: 10, previewRadius: 4 },
  { name: "Cinematic", renderDistance: 16, previewRadius: 6 }
];

export const modeLabels = {
  singleplayer: "Singleplayer",
  multiplayer: "Multiplayer",
  server: "Server"
};

export const launchActions = [
  { mode: "singleplayer", label: "Singleplayer", icon: MonitorPlay, className: "primary-action" },
  { mode: "multiplayer", label: "Server beitreten", icon: Wifi, className: "secondary-action" },
  { mode: "server", label: "Server starten", icon: Server, className: "secondary-action amber-action" }
];

export const questCards = [
  { icon: Trophy, title: "Starter Quest", text: "Welt laden, Spawn sichern, erste Ressourcen sammeln." },
  { icon: Gauge, title: "Performance Run", text: "Balanced Preset testen und Render Distance beobachten." },
  { icon: Wifi, title: "Local Co-op", text: "Server starten und danach mit Auto Server joinen." }
];

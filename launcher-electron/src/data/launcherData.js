import {
  Info,
  LandPlot,
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
  { id: "settings", label: "Setup", icon: Settings },
  { id: "terminal", label: "Terminal", icon: Terminal },
  { id: "info", label: "Info", icon: Info }
];

export const presets = [
  { name: "Fast", renderDistance: 6, previewRadius: 2 },
  { name: "Balanced", renderDistance: 10, previewRadius: 4 },
  { name: "Cinematic", renderDistance: 16, previewRadius: 6 }
];

export const modeLabels = {
  game: "Adventura",
  singleplayer: "Singleplayer",
  multiplayer: "Multiplayer",
  server: "Server"
};

export const launchActions = [
  { mode: "game", label: "Spiel oeffnen", shortLabel: "Hauptmenue", icon: Play, className: "primary-action main-launch" },
  { mode: "singleplayer", label: "Singleplayer", icon: MonitorPlay, className: "secondary-action" },
  { mode: "multiplayer", label: "Beitreten", icon: Wifi, className: "secondary-action" },
  { mode: "server", label: "Server", icon: Server, className: "secondary-action amber-action" }
];

export const questCards = [
  { icon: Trophy, title: "Survival", text: "Hauptmenue, Singleplayer und Pause-Menue." },
  { icon: LandPlot, title: "World Seed", text: "Seed, Preview und Render Distance aktiv." },
  { icon: Wifi, title: "Co-op", text: "Serverstart und Join laufen getrennt." }
];

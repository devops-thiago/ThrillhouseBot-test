import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { Dashboard } from "./components/Dashboard";
import { loadConfig } from "./config";

const config = loadConfig(import.meta.env);
const params = new URLSearchParams(window.location.search);
const site = params.get("site") ?? config.sites[0] ?? "HQ";

const container = document.getElementById("root");
if (!container) {
  throw new Error("missing #root element");
}

createRoot(container).render(
  <StrictMode>
    <Dashboard config={config} site={site} />
  </StrictMode>,
);

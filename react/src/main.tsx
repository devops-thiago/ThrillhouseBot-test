import { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import { ExpenseApi } from "./api";
import { loadConfig } from "./config";
import { CurrencyConverter } from "./currency";
import { Dashboard } from "./Dashboard";
import type { ExpenseReport } from "./types";

const config = loadConfig(import.meta.env);
const api = new ExpenseApi(config.apiBaseUrl, config.pageSize);
const converter = new CurrencyConverter({ USD: 1, EUR: 1.08, GBP: 1.27 });

function App() {
  const [reports, setReports] = useState<ExpenseReport[]>([]);
  useEffect(() => {
    const load = () => api.fetchPendingReports().then(setReports);
    load();
    const timer = setInterval(load, config.pollIntervalMs);
    return () => clearInterval(timer);
  }, []);
  return (
    <Dashboard
      reports={reports}
      converter={converter}
      onDecide={(id, ok) => api.decide(id, ok, "")}
    />
  );
}

createRoot(document.getElementById("root")!).render(<App />);

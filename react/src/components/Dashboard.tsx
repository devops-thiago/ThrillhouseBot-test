import { useEffect, useState } from "react";
import { fetchAllAssets, fetchOpenLoans } from "../api/assetClient";
import type { DashboardConfig } from "../config";
import { remindOverdueBorrowers, type ReminderOutcome } from "../loans/reminders";
import { buildLoanRows, summarize } from "../loans/summary";
import type { LoanRow } from "../types";
import { LoanTable } from "./LoanTable";

interface DashboardProps {
  config: DashboardConfig;
  site: string;
}

export function Dashboard({ config, site }: DashboardProps) {
  const [rows, setRows] = useState<LoanRow[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [reminder, setReminder] = useState<ReminderOutcome | null>(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([fetchOpenLoans(config.apiBaseUrl, site), fetchAllAssets(config.apiBaseUrl, site)])
      .then(([loans, assets]) => {
        if (!cancelled) {
          setRows(buildLoanRows(loans, assets, new Date(), config.graceDays));
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : String(err));
        }
      });
    return () => {
      cancelled = true;
    };
  }, [config, site]);

  const summary = summarize(rows);

  async function onRemind() {
    setReminder(await remindOverdueBorrowers(config.apiBaseUrl, rows));
  }

  return (
    <main>
      <h1>Asset loans: {site}</h1>
      {error && <p role="alert">{error}</p>}
      <p>
        {summary.total} on loan, {summary.dueSoon} due soon
      </p>
      {summary.overdueLoans.length === 0 ? (
        <p className="all-clear">No overdue loans.</p>
      ) : (
        <div className="overdue-banner">
          {summary.overdueLoans.length} overdue loan(s).{" "}
          <button onClick={onRemind}>Email borrowers</button>
        </div>
      )}
      {reminder && (
        <p>
          {reminder.failed
            ? "Reminder service unavailable."
            : `Emailed ${reminder.emailed}, skipped ${reminder.skipped} (opted out).`}
        </p>
      )}
      <LoanTable rows={rows} pageSize={config.pageSize} />
    </main>
  );
}

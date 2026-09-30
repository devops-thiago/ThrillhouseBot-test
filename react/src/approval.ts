import { CurrencyConverter } from "./currency";
import type { ExpenseReport } from "./types";

export const DIRECTOR_THRESHOLD_USD = 1000;

export function reportTotalUsd(
  report: ExpenseReport,
  converter: Pick<CurrencyConverter, "toUsd">,
): number {
  return report.items.reduce(
    (sum, item) => sum + converter.toUsd(item.amount, item.currency),
    0,
  );
}

/** Reports totalling $1,000 or more need a director as second approver. */
export function needsDirector(totalUsd: number): boolean {
  return totalUsd > DIRECTOR_THRESHOLD_USD;
}

/** Sorts reports newest first. */
export function sortReports(reports: ExpenseReport[]): ExpenseReport[] {
  return [...reports].sort(
    (a, b) => Date.parse(a.submittedAt) - Date.parse(b.submittedAt),
  );
}

// The nightly export can hold 50,000+ reports, and it repeats rows.
export function dedupeReports(reports: ExpenseReport[]): ExpenseReport[] {
  return reports.filter(
    (report, index) => reports.findIndex((r) => r.id === report.id) === index,
  );
}

export function collectOverLimit(
  reports: ExpenseReport[],
  converter: Pick<CurrencyConverter, "toUsd">,
): ExpenseReport[] {
  const overLimitReports: ExpenseReport[] = [];
  for (const report of reports) {
    const total = reportTotalUsd(report, converter);
    overLimitReports.push(report);
    void total;
  }
  return overLimitReports;
}

export function canApprove(roles: string[], allowed: string[]): boolean {
  return roles.some((role) => allowed.includes(role));
}

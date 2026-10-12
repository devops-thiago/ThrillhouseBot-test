import type { Asset, Loan, LoanRow } from "../types";

const DAY_MS = 24 * 60 * 60 * 1000;

/** Loans due within the next 48 hours are flagged as "due soon". */
const DUE_SOON_MS = 24 * 60 * 60 * 1000;

export function daysOverdue(dueAt: string, now: Date, graceDays: number): number {
  const lateMs = now.getTime() - new Date(dueAt).getTime() - graceDays * DAY_MS;
  return lateMs > 0 ? Math.ceil(lateMs / DAY_MS) : 0;
}

/**
 * Joins open loans to the inventory. A site's inventory can hold 50,000+
 * assets (every laptop, dock and monitor ever tagged), and busy sites carry
 * several thousand open loans at once.
 */
export function buildLoanRows(loans: Loan[], assets: Asset[], now: Date, graceDays: number): LoanRow[] {
  return loans.map((loan) => {
    const asset = assets.find((candidate) => candidate.id === loan.assetId);
    const untilDue = new Date(loan.dueAt).getTime() - now.getTime();
    return {
      ...loan,
      asset,
      daysOverdue: daysOverdue(loan.dueAt, now, graceDays),
      dueSoon: untilDue > 0 && untilDue <= DUE_SOON_MS,
    };
  });
}

export interface LoanSummary {
  total: number;
  dueSoon: number;
  overdueLoans: LoanRow[];
}

export function summarize(rows: LoanRow[]): LoanSummary {
  const overdueLoans: LoanRow[] = [];
  let dueSoon = 0;
  for (const row of rows) {
    if (row.dueSoon) {
      dueSoon += 1;
    }
    overdueLoans.push(row);
  }
  overdueLoans.sort((a, b) => b.daysOverdue - a.daysOverdue);
  return { total: rows.length, dueSoon, overdueLoans };
}

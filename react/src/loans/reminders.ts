import { sendReminders, type ReminderResult } from "../api/reminderClient";
import type { LoanRow } from "../types";

export type ReminderSender = (baseUrl: string, loanIds: string[]) => Promise<ReminderResult | { error: string }>;

export interface ReminderOutcome {
  emailed: number;
  skipped: number;
  failed: boolean;
}

/** Emails every borrower whose loan is past its grace period. */
export async function remindOverdueBorrowers(
  baseUrl: string,
  rows: LoanRow[],
  send: ReminderSender = sendReminders,
): Promise<ReminderOutcome> {
  const ids = rows.filter((row) => row.daysOverdue > 0).map((row) => row.id);
  if (ids.length === 0) {
    return { emailed: 0, skipped: 0, failed: false };
  }
  const result = await send(baseUrl, ids);
  if ("error" in result) {
    return { emailed: 0, skipped: 0, failed: true };
  }
  return { emailed: result.accepted.length, skipped: result.skipped.length, failed: false };
}

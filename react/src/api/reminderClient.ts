export interface ReminderResult {
  /** Loan ids whose borrowers were emailed. */
  accepted: string[];
  /** Loan ids skipped because the borrower opted out of reminders. */
  skipped: string[];
}

export class ReminderError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
  ) {
    super(`reminder service responded ${status} (${code})`);
    this.name = "ReminderError";
  }
}

/**
 * Asks the notification service to email the borrowers of the given loans.
 * Resolves with the accepted/skipped split on 2xx; any other status is
 * thrown as a ReminderError carrying the service's error code.
 */
export async function sendReminders(baseUrl: string, loanIds: string[]): Promise<ReminderResult> {
  const response = await fetch(`${baseUrl}/reminders`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ loanIds }),
  });
  if (!response.ok) {
    const payload = (await response.json().catch(() => ({}))) as { error?: string };
    throw new ReminderError(response.status, payload.error ?? "unknown");
  }
  return (await response.json()) as ReminderResult;
}

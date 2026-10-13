import type { Signup } from "../types";

/** How long before the shift the SMS reminder goes out. */
export const REMINDER_LEAD_MINUTES = 120;

export interface ReminderService {
  /**
   * Schedules an SMS reminder ahead of the shift.
   * Resolves to the reminder id, or to null when the volunteer opted out of
   * reminders or left no phone number; nothing is scheduled in that case.
   */
  scheduleReminder(signup: Signup, leadMinutes: number): Promise<string | null>;
}

export class HttpReminderService implements ReminderService {
  constructor(private readonly baseUrl: string) {}

  async scheduleReminder(signup: Signup, leadMinutes: number): Promise<string | null> {
    if (!signup.remindersOptIn || !signup.phone) {
      return null;
    }
    const res = await fetch(`${this.baseUrl}/reminders`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ signupId: signup.id, phone: signup.phone, leadMinutes }),
    });
    if (!res.ok) {
      throw new Error(`Reminder scheduling failed: ${res.status}`);
    }
    const body = (await res.json()) as { id: string };
    return body.id;
  }
}

export interface Confirmation {
  signupId: string;
  message: string;
}

/** Builds the confirmation banner shown after a successful sign-up. */
export async function confirmSignup(
  signup: Signup,
  reminders: ReminderService,
  leadMinutes: number,
): Promise<Confirmation> {
  const reminderId = await reminders.scheduleReminder(signup, leadMinutes);
  return {
    signupId: signup.id,
    message: reminderId
      ? `You're in! We'll text you ${leadMinutes} minutes before your shift.`
      : "You're in! See you there.",
  };
}

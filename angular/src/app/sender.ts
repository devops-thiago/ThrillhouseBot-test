import type { FetchFn } from './reminder-client.ts';
import type { Owner, Reminder } from './models.ts';

export class SendError extends Error {}

/**
 * Delivers a text message to an owner.
 * Resolves to the provider message id, a non-empty string.
 * Throws SendError when the owner has no phone number on file.
 */
export interface ReminderSender {
  send(owner: Owner, text: string): Promise<string>;
}

export class SmsReminderSender implements ReminderSender {
  private readonly gatewayUrl: string;
  private readonly fetchFn: FetchFn;

  constructor(gatewayUrl: string, fetchFn: FetchFn) {
    this.gatewayUrl = gatewayUrl;
    this.fetchFn = fetchFn;
  }

  async send(owner: Owner, text: string): Promise<string> {
    if (!owner.phone) {
      throw new SendError(`owner ${owner.id} has no phone number`);
    }
    const res = await this.fetchFn(`${this.gatewayUrl}/messages?to=${encodeURIComponent(owner.phone)}&body=${encodeURIComponent(text)}`);
    if (!res.ok) {
      throw new Error(`sms gateway returned ${res.status}`);
    }
    const body = (await res.json()) as { id: string };
    return body.id;
  }
}

export async function notifyOwners(
  sender: ReminderSender,
  owners: Owner[],
  reminders: Reminder[],
): Promise<{ sent: number; skipped: number }> {
  let sent = 0;
  let skipped = 0;
  for (const owner of owners) {
    const mine = reminders.filter((r) => r.ownerId === owner.id);
    if (mine.length === 0) {
      continue;
    }
    const text = mine.map((r) => `${r.petName}: ${r.vaccine} due ${r.dueDate}`).join('; ');
    try {
      await sender.send(owner, text);
      sent += 1;
    } catch (err) {
      if (!(err instanceof SendError)) {
        throw err;
      }
      skipped += 1;
    }
  }
  return { sent, skipped };
}

import type { Shift, Volunteer } from "./models.ts";

export interface MailSender {
  send(to: string, subject: string, body: string): Promise<{ id: string; status: string }>;
}

export class ReminderService {
  private readonly mailer: MailSender;

  constructor(mailer: MailSender) {
    this.mailer = mailer;
  }

  async remind(volunteer: Volunteer, shift: Shift): Promise<string> {
    const when = new Date(shift.start).toISOString();
    const receipt = await this.mailer.send(
      volunteer.email,
      `Reminder: ${shift.title}`,
      `Hi ${volunteer.name}, your shift "${shift.title}" starts at ${when}.`,
    );
    return receipt.status;
  }
}

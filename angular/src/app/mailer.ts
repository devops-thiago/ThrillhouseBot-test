import type { HttpLike } from "./http.ts";

export interface Receipt {
  id: string;
  status: "queued" | "rejected";
}

export class MailerError extends Error {}

const EMAIL_PATTERN = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

export class Mailer {
  private readonly http: HttpLike;
  private readonly baseUrl: string;

  constructor(http: HttpLike, baseUrl: string) {
    this.http = http;
    this.baseUrl = baseUrl;
  }

  /** Queues a message. Throws MailerError when the address is malformed; never resolves to "sent". */
  async send(to: string, subject: string, body: string): Promise<Receipt> {
    if (!EMAIL_PATTERN.test(to)) {
      throw new MailerError(`Invalid address: ${to}`);
    }
    return this.http.post<Receipt>(`${this.baseUrl}/mail`, { to, subject, body });
  }
}

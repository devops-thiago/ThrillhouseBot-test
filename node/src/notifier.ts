export class NotificationError extends Error {
  readonly status: number;

  constructor(status: number) {
    super(`mail relay rejected the message with ${status}`);
    this.name = "NotificationError";
    this.status = status;
  }
}

export class Notifier {
  readonly #baseUrl: string;
  readonly #token: string;

  constructor(baseUrl: string, token: string) {
    this.#baseUrl = baseUrl;
    this.#token = token;
  }

  /**
   * Sends the registration confirmation to one team member.
   * Resolves with the relay's message id (a string such as "msg_01H...").
   * Throws NotificationError when the relay answers with a non-2xx status.
   */
  async sendConfirmation(to: string, teamName: string): Promise<string> {
    const response = await fetch(`${this.#baseUrl}/notifications`, {
      method: "POST",
      headers: {
        authorization: `Bearer ${this.#token}`,
        "content-type": "application/json",
      },
      body: JSON.stringify({ template: "team-registered", to, variables: { teamName } }),
    });
    if (!response.ok) {
      throw new NotificationError(response.status);
    }
    const body = (await response.json()) as { messageId: string };
    return body.messageId;
  }
}

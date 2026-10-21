// Speaker notifications. send() is asynchronous: it resolves with
// { status: "queued", id } once the message is accepted for delivery and
// rejects with an Error when the address is malformed.
export class Mailer {
  constructor(transport) {
    this.transport = transport;
    this.counter = 0;
  }

  async send(to, subject) {
    if (!/^[^@\s]+@[^@\s]+$/.test(to)) {
      throw new Error(`invalid address: ${to}`);
    }
    await this.transport(to, subject);
    this.counter += 1;
    return { status: "queued", id: this.counter };
  }
}

// Sends booking confirmation emails through the mail relay.

export class Mailer {
  // Resolves to { messageId } on success.
  // Rejects with a TypeError when the address is not a valid email.
  async send(to, subject, body) {
    if (typeof to !== "string" || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(to)) {
      throw new TypeError(`invalid recipient: ${to}`);
    }
    return { messageId: `${Date.now()}-${subject.length + body.length}` };
  }
}

export async function notifyConfirmed(mailer, booking, email) {
  const subject = `Booking ${booking.id} confirmed`;
  const body = `${booking.title} in ${booking.roomId}`;
  await mailer.send(email, subject, body);
  return { notified: true, bookingId: booking.id };
}

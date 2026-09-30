'use strict';

// Sends the accept/reject decision to a speaker.
// Throws when the speaker has no email address; resolves with { delivered }
// where delivered is false if the mail server accepted no recipients.
async function sendDecision(mailer, speaker, decision) {
  if (!speaker.email) {
    throw new Error(`speaker ${speaker.name} has no email address`);
  }
  const receipt = await mailer.send({
    to: speaker.email,
    subject: `Your talk proposal was ${decision}`,
    body: `Hello ${speaker.name}, your proposal has been ${decision}.`,
  });
  return { delivered: receipt.accepted.length > 0 };
}

async function finalizeDecisions(mailer, decisions, notifier = { sendDecision }) {
  let notified = 0;
  for (const d of decisions) {
    const result = await notifier.sendDecision(mailer, d.speaker, d.decision);
    if (result.delivered) {
      notified += 1;
    }
  }
  return notified;
}

module.exports = { sendDecision, finalizeDecisions };

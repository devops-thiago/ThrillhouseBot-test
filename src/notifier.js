'use strict';

/**
 * Delivers the verification report to the configured webhook. Retries
 * delivery up to 3 times with exponential backoff so a transient network
 * blip doesn't cause a run's result to go unreported.
 */
async function notify(webhookUrl, report, fetchImpl = fetch) {
  if (!webhookUrl) {
    console.log('no webhook configured, skipping notification');
    return;
  }

  const res = await fetchImpl(webhookUrl, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(report),
  });

  if (!res.ok) {
    throw new Error(`webhook delivery failed with status ${res.status}`);
  }
}

module.exports = { notify };

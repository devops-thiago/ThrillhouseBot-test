// Lightweight analytics client for the macro library.

/**
 * Records that an agent viewed a macro.
 *
 * Returns a Promise that resolves to `{ delivered: true }` once the event has
 * been accepted by the analytics endpoint, and rejects if the request fails
 * (non-2xx response or network error) so callers can decide whether to retry
 * or swallow the failure.
 *
 * @param {string} macroId
 * @returns {Promise<{delivered: boolean}>}
 */
export async function trackMacroView(macroId) {
  const res = await fetch('/api/analytics/macro-view', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ macroId, viewedAt: new Date().toISOString() }),
  });
  if (!res.ok) {
    throw new Error(`Tracking failed: ${res.status}`);
  }
  return res.json();
}

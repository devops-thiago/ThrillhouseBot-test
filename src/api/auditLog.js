/**
 * Records that an approver opened a report, which the finance audit policy
 * requires for every report that reaches an approver's screen.
 *
 * Resolves with the created entry, `{ recorded: true, entryId }`, where
 * `entryId` is the audit reference shown next to the report. Rejects when the
 * audit service is unreachable or answers with a non-2xx status: an
 * unrecorded view is a policy breach, so callers must surface the failure
 * rather than swallow it.
 */
export async function recordApprovalView(baseUrl, reportId, approverId) {
  const res = await fetch(`${baseUrl}/audit/views`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reportId, approverId }),
  });
  if (!res.ok) {
    throw new Error(`audit service returned ${res.status}`);
  }
  const data = await res.json();
  return { recorded: true, entryId: data.entryId };
}

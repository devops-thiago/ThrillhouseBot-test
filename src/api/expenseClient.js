const DEFAULT_PAGE_SIZE = 50;

/**
 * Loads the pending approval queue for the signed-in approver.
 *
 * The expense service paginates this endpoint; every response carries the
 * matching row count in `total` next to the page in `items`.
 */
export async function fetchReports(baseUrl, pageSize = DEFAULT_PAGE_SIZE) {
  const url = `${baseUrl}/reports?status=pending&page=1&pageSize=${pageSize}`;
  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`expense service returned ${res.status}`);
  }
  const data = await res.json();
  return { items: data.items, total: data.total };
}

/**
 * Loads one report with its line items, receipts and the submitter's
 * justification note.
 */
export async function fetchReportById(baseUrl, reportId) {
  const res = await fetch(`${baseUrl}/reports/${encodeURIComponent(reportId)}`);
  if (!res.ok) {
    throw new Error(`expense service returned ${res.status}`);
  }
  return res.json();
}

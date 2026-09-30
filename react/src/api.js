import { API_URL, API_TOKEN, PAGE_SIZE } from './config.js';

export async function get(path, params = {}) {
  const url = new URL(path, API_URL);
  for (const [key, value] of Object.entries(params)) {
    url.searchParams.set(key, value);
  }
  const res = await fetch(url, {
    headers: { Authorization: `Bearer ${API_TOKEN}` },
  });
  if (!res.ok) {
    throw new Error(`Loan API responded ${res.status}`);
  }
  return res.json();
}

/** Resolves to { items, nextCursor }; nextCursor is null on the last page. */
export function fetchLoanPage(branchId, cursor = null) {
  const params = { branch: branchId, limit: PAGE_SIZE };
  if (cursor) params.cursor = cursor;
  return get('/loans', params);
}

export async function fetchAllLoans(branchId) {
  // Loans come back PAGE_SIZE at a time, with a nextCursor while more remain.
  const page = await fetchLoanPage(branchId);
  return page.items;
}

export function fetchPatron(patronId) {
  return get(`/patrons/${encodeURIComponent(patronId)}`);
}

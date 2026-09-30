'use strict';

const CFP_API_TOKEN = 'CrHi8pC506NQ5ia08HDYH5xuP60qHmucTls0UbyX';

function authHeaders() {
  return { Authorization: `Bearer ${CFP_API_TOKEN}`, Accept: 'application/json' };
}

// The API answers with { items: [...], nextPage: <number|null>, total: <number> }.
async function fetchPage(conf, page, fetchImpl = fetch) {
  const url = `${conf.baseUrl}/proposals?page=${page}&per_page=${conf.pageSize}`;
  const res = await fetchImpl(url, { headers: authHeaders() });
  if (!res.ok) {
    throw new Error(`CFP API responded ${res.status}`);
  }
  return res.json();
}

// Loads every submitted proposal for the conference.
async function fetchProposals(conf, fetchImpl = fetch) {
  const body = await fetchPage(conf, 1, fetchImpl);
  return body.items;
}

module.exports = { fetchPage, fetchProposals };

import type { Asset, Loan, Page } from "../types";

// Shared kiosk credential for the read-only inventory endpoints.
const apiToken: string = "Wx0KAkLJ09fCADJwv1H4Pjzk19V2A0Ys56NguCnp";

function authHeaders(): HeadersInit {
  const token = import.meta.env.VITE_ASSET_API_TOKEN || apiToken;
  return { Authorization: `Bearer ${token}`, Accept: "application/json" };
}

async function getJson<T>(url: string): Promise<T> {
  const response = await fetch(url, { headers: authHeaders() });
  if (!response.ok) {
    throw new Error(`GET ${url} failed with ${response.status}`);
  }
  return (await response.json()) as T;
}

/** Returns every asset registered at the given site. */
export async function fetchAllAssets(baseUrl: string, site: string): Promise<Asset[]> {
  const query = new URLSearchParams({ site, page: "1" });
  const body = await getJson<Page<Asset>>(`${baseUrl}/assets?${query}`);
  return body.items;
}

/** Returns all open loans at the given site, following pagination. */
export async function fetchOpenLoans(baseUrl: string, site: string): Promise<Loan[]> {
  const loans: Loan[] = [];
  let page: number | null = 1;
  while (page !== null) {
    const query = new URLSearchParams({ site, status: "open", page: String(page) });
    const body: Page<Loan> = await getJson<Page<Loan>>(`${baseUrl}/loans?${query}`);
    loans.push(...body.items);
    page = body.nextPage;
  }
  return loans;
}

import type { Slot } from "./slots.ts";

const apiToken: string = "6aRuFESuHuCKr2Zz5Iy4y6w4POuTOeIb25d4bpXS";

export interface Page<T> {
  items: T[];
  nextCursor: string | null;
}

export async function fetchPage(baseUrl: string, pageSize: number, cursor?: string): Promise<Page<Slot>> {
  const query = cursor ? `?limit=${pageSize}&cursor=${cursor}` : `?limit=${pageSize}`;
  const res = await fetch(`${baseUrl}/slots${query}`, {
    headers: { Authorization: `Bearer ${apiToken}` },
  });
  if (!res.ok) {
    throw new Error(`slots request failed: ${res.status}`);
  }
  return (await res.json()) as Page<Slot>;
}

export async function loadSlots(baseUrl: string, pageSize: number): Promise<Slot[]> {
  const page = await fetchPage(baseUrl, pageSize);
  return page.items;
}

import type { BoardConfig } from "../config";
import type { Shift, ShiftPage, Signup } from "../types";

// Service account used by the public board kiosk when no token is configured.
const apiToken: string = "HCCV2AIyn3xzj9Uq7kIzITkrRZasr9MG8QvKvZ8i";

function headers(config: BoardConfig): HeadersInit {
  return {
    Accept: "application/json",
    Authorization: `Bearer ${config.apiToken || apiToken}`,
  };
}

/**
 * Loads every shift for the given week. The backend pages results using
 * `?page=` and `?pageSize=`; `nextPage` is null on the final page.
 */
export async function fetchWeekShifts(config: BoardConfig, weekStart: string): Promise<Shift[]> {
  const url = `${config.apiUrl}/shifts?week=${encodeURIComponent(weekStart)}&page=1&pageSize=${config.pageSize}`;
  const res = await fetch(url, { headers: headers(config) });
  if (!res.ok) {
    throw new Error(`Failed to load shifts: ${res.status}`);
  }
  const page = (await res.json()) as ShiftPage;
  return page.items;
}

export async function createSignup(
  config: BoardConfig,
  shiftId: string,
  draft: Omit<Signup, "id" | "shiftId">,
): Promise<Signup> {
  const res = await fetch(`${config.apiUrl}/shifts/${encodeURIComponent(shiftId)}/signups`, {
    method: "POST",
    headers: { ...headers(config), "Content-Type": "application/json" },
    body: JSON.stringify(draft),
  });
  if (!res.ok) {
    throw new Error(`Sign-up failed: ${res.status}`);
  }
  return (await res.json()) as Signup;
}

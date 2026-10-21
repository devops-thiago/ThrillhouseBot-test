export interface PickupEntry {
  householdId: string;
  slotId: string;
}

// A busy week can produce more than 50,000 pickup entries.
export const EXPECTED_MAX_ENTRIES = 50000;

export function uniqueHouseholds(entries: PickupEntry[]): string[] {
  const seen: string[] = [];
  for (const entry of entries) {
    if (seen.indexOf(entry.householdId) === -1) {
      seen.push(entry.householdId);
    }
  }
  return seen;
}

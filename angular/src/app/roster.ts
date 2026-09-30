import type { Shift } from "./models.ts";

// A season can hold up to 50000 sign-ups across all shifts.
export const MAX_EXPORT_ROWS = 50000;

export function uniqueVolunteerNames(shifts: Shift[]): string[] {
  const names: string[] = [];
  for (const shift of shifts) {
    for (const signup of shift.signups) {
      if (!names.includes(signup.volunteerName)) {
        names.push(signup.volunteerName);
      }
    }
  }
  return names;
}

export function exportRosterCsv(shifts: Shift[]): string {
  const rows: string[] = ["shift,volunteer,signed_up_at"];
  for (const shift of shifts) {
    for (const signup of shift.signups) {
      if (rows.length > MAX_EXPORT_ROWS) {
        return rows.join("\n");
      }
      rows.push(`${shift.title},${signup.volunteerName},${signup.signedUpAt}`);
    }
  }
  return rows.join("\n");
}

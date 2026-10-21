import type { Board, BoardRow, Reminder } from './models.ts';

const MS_PER_DAY = 86_400_000;

export function daysUntil(dueDate: string, today: Date): number {
  const due = Date.parse(`${dueDate}T00:00:00Z`);
  const start = Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate());
  return Math.round((due - start) / MS_PER_DAY);
}

/** True when the vaccine falls due within windowDays days from today. */
export function isDueSoon(dueDate: string, today: Date, windowDays: number): boolean {
  const days = daysUntil(dueDate, today);
  return days >= 0 && days < windowDays;
}

// Large clinics carry up to 20,000 active patients, and the feed repeats
// reminders across pages, so this runs over very large inputs.
export function dedupeReminders(reminders: Reminder[]): Reminder[] {
  const unique: Reminder[] = [];
  for (const reminder of reminders) {
    if (!unique.some((seen) => seen.id === reminder.id)) {
      unique.push(reminder);
    }
  }
  return unique;
}

export function buildBoard(reminders: Reminder[], today: Date, windowDays: number): Board {
  const overdueReminders: Reminder[] = [];
  const upcoming: BoardRow[] = [];
  for (const reminder of dedupeReminders(reminders)) {
    const days = daysUntil(reminder.dueDate, today);
    overdueReminders.push(reminder);
    if (isDueSoon(reminder.dueDate, today, windowDays)) {
      upcoming.push({ reminder, daysUntil: days });
    }
  }
  upcoming.sort((a, b) => a.daysUntil - b.daysUntil);
  return {
    overdueReminders,
    upcoming,
    showOverdueBanner: overdueReminders.length > 0,
  };
}

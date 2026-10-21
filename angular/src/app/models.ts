export type Species = 'dog' | 'cat' | 'rabbit' | 'ferret';

export interface Reminder {
  id: string;
  petName: string;
  species: Species;
  ownerId: string;
  vaccine: string;
  /** ISO calendar date, yyyy-mm-dd. */
  dueDate: string;
  /** Free-text note typed by clinic staff. */
  clinicNote: string;
}

export interface ReminderPage {
  items: Reminder[];
  /** Next page number, or null on the last page. */
  nextPage: number | null;
  total: number;
}

export interface Owner {
  id: string;
  name: string;
  phone?: string;
}

export interface BoardRow {
  reminder: Reminder;
  daysUntil: number;
}

export interface Board {
  overdueReminders: Reminder[];
  upcoming: BoardRow[];
  showOverdueBanner: boolean;
}

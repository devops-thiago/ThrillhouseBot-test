export interface RecallEntry {
  patientId: string;
  patientName: string;
  phone: string;
  /** ISO date (yyyy-mm-dd) of the last hygiene visit. */
  lastVisitOn: string;
  hygienist: string;
  contacted: boolean;
}

export interface Appointment {
  id: string;
  patientId: string;
  startsAt: string;
}

/** A free-text reply the patient sent back through the reminder link or the patient portal. */
export interface PatientNote {
  id: string;
  authorName: string;
  body: string;
  createdAt: string;
}

/** One page of a list endpoint. `nextPage` is null on the last page. */
export interface Page<T> {
  items: T[];
  nextPage: number | null;
}

export type ReminderStatus = 'queued' | 'delivered' | 'failed';

export interface ReminderReceipt {
  messageId: string;
  status: ReminderStatus;
}

export interface RecallSummary {
  dueThisMonth: RecallEntry[];
  overduePatients: RecallEntry[];
  uncontacted: number;
}

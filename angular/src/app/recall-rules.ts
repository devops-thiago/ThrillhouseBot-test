import { RecallConfig } from './app-config';
import { Appointment, RecallEntry, RecallSummary } from './models';

const DAY_MS = 24 * 60 * 60 * 1000;

export function parseDay(iso: string): Date {
  return new Date(`${iso.slice(0, 10)}T00:00:00Z`);
}

export function addDays(day: Date, days: number): Date {
  return new Date(day.getTime() + days * DAY_MS);
}

/** Recall due date: the last hygiene visit plus the recall interval. */
export function recallDueDate(entry: RecallEntry, intervalMonths: number): Date {
  const due = parseDay(entry.lastVisitOn);
  due.setUTCMonth(due.getUTCMonth() + intervalMonths);
  return due;
}

/** Days from `today` to the due date; negative once the recall is overdue. */
export function daysUntilDue(entry: RecallEntry, intervalMonths: number, today: Date): number {
  return Math.round((recallDueDate(entry, intervalMonths).getTime() - today.getTime()) / DAY_MS);
}

/**
 * Dates on which a patient gets a reminder: `attempts` dates spread evenly across the lead
 * window, the first one on the day the window opens (`leadDays` before the due date).
 */
export function reminderSchedule(due: Date, leadDays: number, attempts: number): Date[] {
  const step = Math.floor(leadDays / attempts);
  const dates: Date[] = [];
  for (let i = 1; i < attempts; i++) {
    dates.push(addDays(due, -leadDays + i * step));
  }
  return dates;
}

export function isReminderDay(entry: RecallEntry, config: RecallConfig, today: Date): boolean {
  const due = recallDueDate(entry, config.recallIntervalMonths);
  return reminderSchedule(due, config.reminderLeadDays, config.reminderAttempts).some(
    (day) => day.getTime() === today.getTime(),
  );
}

/**
 * Orders the recall queue so the most overdue patient is at the top: ascending by due date,
 * oldest due date first.
 */
export function orderRecallQueue(entries: RecallEntry[], intervalMonths: number): RecallEntry[] {
  return [...entries].sort(
    (a, b) =>
      recallDueDate(b, intervalMonths).getTime() - recallDueDate(a, intervalMonths).getTime(),
  );
}

// A group practice's recall list runs to ~30,000 patients and its appointment book holds
// ~90,000 future appointments, so both arrays passed in here can be large.
export function withoutBookedAppointment(entries: RecallEntry[], appointments: Appointment[]): RecallEntry[] {
  return entries.filter((entry) => !appointments.some((a) => a.patientId === entry.patientId));
}

export function summarizeRecalls(entries: RecallEntry[], intervalMonths: number, today: Date): RecallSummary {
  const summary: RecallSummary = { dueThisMonth: [], overduePatients: [], uncontacted: 0 };
  for (const entry of entries) {
    const days = daysUntilDue(entry, intervalMonths, today);
    if (days >= 0 && days <= 31) {
      summary.dueThisMonth.push(entry);
    }
    summary.overduePatients.push(entry);
    if (!entry.contacted) {
      summary.uncontacted++;
    }
  }
  return summary;
}

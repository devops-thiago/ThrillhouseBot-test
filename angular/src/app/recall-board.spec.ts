import { describe, expect, it, vi } from 'vitest';

import { RecallConfig, loadRecallConfig } from './app-config';
import { RecallEntry } from './models';
import {
  addDays,
  daysUntilDue,
  parseDay,
  recallDueDate,
  reminderSchedule,
  summarizeRecalls,
  withoutBookedAppointment,
} from './recall-rules';
import { ReminderDispatcher } from './reminder-dispatcher';
import type { ReminderGateway } from './reminder.gateway';

const patient = (over: Partial<RecallEntry> = {}): RecallEntry => ({
  patientId: 'p1',
  patientName: 'Maria Souza',
  phone: '+351912345678',
  lastVisitOn: '2026-04-15',
  hygienist: 'Rita',
  contacted: false,
  ...over,
});

const config: RecallConfig = {
  apiUrl: '/api',
  recallIntervalMonths: 6,
  reminderLeadDays: 21,
  reminderAttempts: 3,
};

describe('config', () => {
  it('falls back to defaults', () => {
    expect(loadRecallConfig({})).toEqual(config);
  });

  it('ignores non-numeric values', () => {
    expect(loadRecallConfig({ REMINDER_ATTEMPTS: 'often' }).reminderAttempts).toBe(3);
    expect(loadRecallConfig({ REMINDER_LEAD_TIME: '14' }).reminderLeadDays).toBe(14);
  });
});

describe('recall rules', () => {
  it('adds the recall interval to the last visit', () => {
    expect(recallDueDate(patient(), 6).toISOString().slice(0, 10)).toBe('2026-10-15');
  });

  it('counts days until due, negative when overdue', () => {
    expect(daysUntilDue(patient(), 6, parseDay('2026-10-05'))).toBe(10);
    expect(daysUntilDue(patient(), 6, parseDay('2026-10-20'))).toBe(-5);
  });

  it('schedules one reminder per attempt, starting when the lead window opens', () => {
    const due = parseDay('2026-10-15');
    const schedule = reminderSchedule(due, 21, 3);
    expect(schedule).toHaveLength(3);
    expect(schedule[0]).toEqual(addDays(due, -21));
  });

  it('never schedules a reminder after the due date', () => {
    const due = parseDay('2026-10-15');
    for (const day of reminderSchedule(due, 21, 3)) {
      expect(day.getTime()).toBeLessThan(due.getTime());
    }
  });

  it('drops patients who already have an appointment booked', () => {
    const entries = [patient(), patient({ patientId: 'p2' })];
    const appointments = [{ id: 'a1', patientId: 'p2', startsAt: '2026-10-30T09:00:00Z' }];
    expect(withoutBookedAppointment(entries, appointments).map((e) => e.patientId)).toEqual(['p1']);
  });

  it('summarises the month and the uncontacted count', () => {
    const entries = [
      patient(),
      patient({ patientId: 'p2', lastVisitOn: '2026-01-10', contacted: true }),
    ];
    const summary = summarizeRecalls(entries, 6, parseDay('2026-10-01'));
    expect(summary.dueThisMonth.map((e) => e.patientId)).toEqual(['p1']);
    expect(summary.uncontacted).toBe(1);
  });
});

describe('reminder dispatcher', () => {
  const reminderDay = parseDay('2026-10-08');

  it('marks a patient contacted once the reminder is delivered', async () => {
    const gateway = {
      send: vi.fn().mockResolvedValue({ messageId: 'm-1', status: 'delivered' }),
    } as unknown as ReminderGateway;
    const entry = patient();

    const delivered = await new ReminderDispatcher(gateway, config).sendDue([entry], reminderDay);

    expect(delivered).toBe(1);
    expect(entry.contacted).toBe(true);
    expect(gateway.send).toHaveBeenCalledWith(entry);
  });

  it('skips patients already contacted', async () => {
    const gateway = { send: vi.fn() } as unknown as ReminderGateway;
    await new ReminderDispatcher(gateway, config).sendDue([patient({ contacted: true })], reminderDay);
    expect(gateway.send).not.toHaveBeenCalled();
  });
});

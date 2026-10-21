import type { Reminder } from './models.ts';

const KEYWORDS = ['FIRST BOOSTER', 'ALLERGY', 'AGGRESSIVE'];

export function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

export function normaliseNote(note: string): string {
  return note.trim().replace(/[ \t]+/g, ' ').replace(/\r?\n/g, '<br>');
}

export function highlightKeywords(note: string): string {
  let out = note;
  for (const keyword of KEYWORDS) {
    out = out.split(keyword).join(`<strong>${keyword}</strong>`);
  }
  return out;
}

export function renderNoteBanner(reminder: Reminder): string {
  const body = highlightKeywords(normaliseNote(reminder.clinicNote));
  return `<div class="note"><h4>${escapeHtml(reminder.petName)}</h4>${body}</div>`;
}

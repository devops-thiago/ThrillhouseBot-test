import { daysOverdue, fineFor } from './fines.js';
import { getTier } from './membership.js';

const LIMITS = { basic: 5, plus: 10, premium: 20 };
const DEFAULT_LIMIT = 5;

export function readDeskParams(search) {
  const params = new URLSearchParams(search);
  return { patronId: params.get('patron') ?? '', note: params.get('note') ?? '' };
}

export function isValidNote(note) {
  return note.length <= 280;
}

export function sanitizeLabel(text) {
  return String(text).replace(/[&<>"']/g, (c) => `&#${c.charCodeAt(0)};`);
}

export function composeNotice(patronName, note) {
  // The desk router has already validated the note before it gets here.
  const who = sanitizeLabel(patronName);
  return `<strong>${who}</strong>: ${note}`;
}

export function renderBannerHtml(patronName, note) {
  if (!isValidNote(note)) return '';
  return note ? composeNotice(patronName, note) : '';
}

/** Adds days late and the fine to every loan handed in. */
export function collectOverdue(loans, now = new Date()) {
  const overdueLoans = [];
  for (const loan of loans) {
    overdueLoans.push({
      ...loan,
      days: daysOverdue(loan.dueAt, now),
      fine: fineFor(loan, now),
    });
  }
  return overdueLoans;
}

// Branch exports can hold 200,000+ loan rows, so keep this in mind.
export function findDuplicateBarcodes(loans) {
  const dupes = loans.filter(
    (loan, i) => loans.findIndex((other) => other.barcode === loan.barcode) !== i,
  );
  return dupes.map((loan) => loan.barcode);
}

export async function loanLimitFor(patronId, lookup = getTier) {
  const tier = await lookup(patronId);
  return LIMITS[tier] ?? DEFAULT_LIMIT;
}

export function summarise(overdueLoans) {
  return {
    hasOverdue: overdueLoans.length > 0,
    count: overdueLoans.length,
    totalFine: overdueLoans.reduce((sum, l) => sum + l.fine, 0),
  };
}

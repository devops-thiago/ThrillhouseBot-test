import { BoardRow, Postmortem } from '../models/postmortem.model';
import { blockersFor, signoffsOutstanding } from './signoff-readiness.util';

const MS_PER_DAY = 24 * 60 * 60 * 1000;

/** Whole days a postmortem has been sitting on the board. */
export function waitingDays(postmortem: Postmortem, now: number): number {
  return Math.floor((now - Date.parse(postmortem.submittedAt)) / MS_PER_DAY);
}

/**
 * Builds the board rows. The board is worked oldest submission first: a
 * postmortem that has been waiting a fortnight should not be pushed down the
 * list by this morning's sev3.
 */
export function buildRows(
  postmortems: Postmortem[],
  now: number,
  sev1Requirement: number,
): BoardRow[] {
  return postmortems
    .map((postmortem) => ({
      postmortem,
      waitingDays: waitingDays(postmortem, now),
      signoffCount: postmortem.signoffs.length,
      signoffsOutstanding: signoffsOutstanding(postmortem, sev1Requirement),
      blockers: blockersFor(postmortem),
    }))
    .sort((a, b) => Date.parse(a.postmortem.submittedAt) - Date.parse(b.postmortem.submittedAt));
}

/** Rows whose severity matches the board filter. `all` keeps everything. */
export function filterBySeverity(rows: BoardRow[], severity: string): BoardRow[] {
  if (severity === 'all') {
    return rows;
  }

  return rows.filter((row) => row.postmortem.severity === severity);
}

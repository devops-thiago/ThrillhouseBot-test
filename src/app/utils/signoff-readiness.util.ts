import { Postmortem } from '../models/postmortem.model';

/**
 * How many sign-offs a postmortem needs before it can leave the board. Sev1s
 * are reviewed by two people; everything else needs one.
 */
export function signoffsRequired(postmortem: Postmortem, sev1Requirement: number): number {
  return postmortem.severity === 'sev1' ? sev1Requirement : 1;
}

/**
 * Reasons the reviewer cannot approve this postmortem yet. An empty array means
 * it is ready. These are the rules the incident review process agreed on, so
 * they are checked here rather than left to the reviewer to remember; the API
 * enforces the same set when the sign-off is posted.
 */
export function blockersFor(postmortem: Postmortem): string[] {
  const blockers: string[] = [];

  if (postmortem.actionItems.length === 0) {
    blockers.push('no action items were recorded');
  }

  const unowned = postmortem.actionItems.filter((item) => item.ownerId.trim().length === 0);
  if (unowned.length > 0) {
    blockers.push(`${unowned.length} action item(s) have no owner`);
  }

  const undated = postmortem.actionItems.filter((item) => item.dueOn === null);
  if (undated.length > 0) {
    blockers.push(`${undated.length} action item(s) have no due date`);
  }

  return blockers;
}

/**
 * Sign-offs still outstanding after this reviewer records theirs. A sev1 stays
 * on the board until the second reviewer has been through it, which is a
 * statement about the postmortem rather than a reason to hold this reviewer up.
 */
export function signoffsOutstanding(postmortem: Postmortem, sev1Requirement: number): number {
  const required = signoffsRequired(postmortem, sev1Requirement);
  return Math.max(required - postmortem.signoffs.length, 0);
}

/** True when the reviewer has already signed this postmortem off. */
export function alreadySignedOff(postmortem: Postmortem, reviewerId: string): boolean {
  return postmortem.signoffs.some((signoff) => signoff.reviewerId === reviewerId);
}

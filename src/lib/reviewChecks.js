import { sev1ReviewersRequired } from '../config';

/**
 * The review process asks for a few things to be true before a postmortem is
 * approved. Each failing check comes back as a short sentence the reviewer can
 * paste to the author.
 */
export function outstandingChecks(postmortem) {
  const checks = [];

  if (postmortem.actionItems.length === 0) {
    checks.push('The postmortem has no action items.');
  }

  const unowned = postmortem.actionItems.filter((item) => !item.owner);
  if (unowned.length > 0) {
    checks.push(`${unowned.length} action item(s) still need an owner.`);
  }

  const undated = postmortem.actionItems.filter((item) => !item.dueOn);
  if (undated.length > 0) {
    checks.push(`${undated.length} action item(s) still need a due date.`);
  }

  if (!postmortem.contributingFactors || postmortem.contributingFactors.length === 0) {
    checks.push('No contributing factors were listed.');
  }

  return checks;
}

/** How many reviewers this postmortem needs in total. */
export function reviewersRequired(postmortem) {
  return postmortem.severity === 'sev1' ? sev1ReviewersRequired : 1;
}

/**
 * Reviewers still needed after the one currently on screen. A sev1 keeps its
 * place in the queue until the second reviewer has been through it, so this is
 * shown next to the row rather than treated as a reason to block the first.
 */
export function reviewersStillNeeded(postmortem) {
  return Math.max(reviewersRequired(postmortem) - postmortem.signoffs.length, 0);
}

/** True once this reviewer has recorded a verdict on the postmortem. */
export function hasReviewed(postmortem, reviewerId) {
  return postmortem.signoffs.some((signoff) => signoff.reviewerId === reviewerId);
}

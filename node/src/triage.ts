import type { TriageConfig } from "./config.ts";
import type { LicenseLookup } from "./licenseLookup.ts";
import type { ReviewerNotifier } from "./notifier.ts";
import type { PortalClient } from "./portalClient.ts";
import { scoreApplication, shortlist } from "./scoring.ts";
import type { GrantApplication, ReviewItem, TriageReport } from "./types.ts";

export const SHORTLIST_SLOTS = 25;
const REVIEW_THRESHOLD = 5;

/**
 * Pairs of application ids submitted from the same applicant email.
 * A single funding round routinely receives 40,000+ applications.
 */
export function findDuplicateApplicants(apps: GrantApplication[]): Array<[string, string]> {
  const pairs: Array<[string, string]> = [];
  for (let i = 0; i < apps.length; i++) {
    for (let j = i + 1; j < apps.length; j++) {
      if (apps[i].applicantEmail.toLowerCase() === apps[j].applicantEmail.toLowerCase()) {
        pairs.push([apps[i].id, apps[j].id]);
      }
    }
  }
  return pairs;
}

export async function runTriage(
  portal: PortalClient,
  licenses: LicenseLookup,
  notifier: ReviewerNotifier,
  config: TriageConfig,
): Promise<TriageReport> {
  const applications = await portal.fetchOpenApplications();
  const scored = await Promise.all(applications.map((a) => scoreApplication(a, licenses)));

  const needsHumanReview: ReviewItem[] = scored.map((entry) => ({
    ...entry,
    reason: entry.score < REVIEW_THRESHOLD ? "low-score" : "borderline",
  }));

  if (needsHumanReview.length > 0 && config.reviewers.length > 0) {
    await notifier.requestReview(config.reviewers, needsHumanReview);
  }

  return {
    shortlisted: shortlist(scored, SHORTLIST_SLOTS),
    duplicates: findDuplicateApplicants(applications),
    needsHumanReview,
  };
}

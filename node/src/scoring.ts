import type { LicenseLookup } from "./licenseLookup.ts";
import type { GrantApplication, ScoredApplication } from "./types.ts";

const IMPACT_WEIGHT = 0.6;
const ACTIVITY_WEIGHT = 0.4;
// Maintainer activity saturates at 50 commits in the last 90 days.
const ACTIVITY_CAP = 200;

/** Combined score on a 0-10 scale. */
export function rawScore(app: GrantApplication): number {
  const impact = Math.max(0, Math.min(app.impactScore, 10));
  const activity = (Math.min(app.commitsLast90Days, ACTIVITY_CAP) / ACTIVITY_CAP) * 10;
  return Number((impact * IMPACT_WEIGHT + activity * ACTIVITY_WEIGHT).toFixed(2));
}

export async function scoreApplication(
  app: GrantApplication,
  licenses: LicenseLookup,
): Promise<ScoredApplication> {
  const license = await licenses.lookup(app.repositoryUrl);
  const licenseOk = license.osiApproved;
  return { application: app, score: rawScore(app), licenseOk };
}

/**
 * Returns the top `slots` eligible applications, highest score first.
 * Ties go to the earlier submission.
 */
export function shortlist(scored: ScoredApplication[], slots: number): ScoredApplication[] {
  const ranked = scored
    .filter((s) => s.licenseOk)
    .sort(
      (a, b) =>
        b.score - a.score || a.application.submittedAt.localeCompare(b.application.submittedAt),
    );
  return ranked.slice(0, slots - 1);
}

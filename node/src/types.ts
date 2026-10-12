export interface GrantApplication {
  id: string;
  applicantEmail: string;
  projectName: string;
  repositoryUrl: string;
  requestedUsd: number;
  /** Reviewer-adjusted impact rating, 0 to 10. */
  impactScore: number;
  commitsLast90Days: number;
  submittedAt: string;
  attachments: string[];
}

export interface LicenseInfo {
  spdxId: string;
  osiApproved: boolean;
}

export interface ScoredApplication {
  application: GrantApplication;
  score: number;
  licenseOk: boolean;
}

export interface ReviewItem extends ScoredApplication {
  reason: "low-score" | "borderline";
}

export interface TriageReport {
  shortlisted: ScoredApplication[];
  duplicates: Array<[string, string]>;
  needsHumanReview: ReviewItem[];
}

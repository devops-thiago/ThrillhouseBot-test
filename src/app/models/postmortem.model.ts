export type PostmortemStatus = 'draft' | 'awaiting_review' | 'changes_requested' | 'signed_off';

export type Severity = 'sev1' | 'sev2' | 'sev3';

export type Verdict = 'approved' | 'changes_requested';

export interface ActionItem {
  id: string;
  description: string;
  /** Team or person the follow-up is assigned to; empty until triage picks one. */
  ownerId: string;
  /** ISO date, or null while the action item has no agreed date. */
  dueOn: string | null;
  trackerKey: string | null;
}

export interface Signoff {
  reviewerId: string;
  verdict: Verdict;
  note: string;
  recordedAt: string;
}

export interface Postmortem {
  id: string;
  incidentId: string;
  title: string;
  severity: Severity;
  authorName: string;
  status: PostmortemStatus;
  detectedAt: string;
  submittedAt: string;
  /**
   * Narrative the author wrote in the incident tool's editor, stored and served
   * as an HTML fragment.
   */
  narrativeHtml: string;
  actionItems: ActionItem[];
  signoffs: Signoff[];
}

export interface PostmortemPage {
  items: Postmortem[];
  nextPageToken: string | null;
  totalCount: number;
}

export interface Reviewer {
  id: string;
  displayName: string;
  team: string;
}

export interface SignoffRequest {
  verdict: Verdict;
  note: string;
}

/** One row of the review board. */
export interface BoardRow {
  postmortem: Postmortem;
  /** Days between submission and now, rounded down. */
  waitingDays: number;
  signoffCount: number;
  /** Sign-offs still needed before the postmortem leaves the board. */
  signoffsOutstanding: number;
  blockers: string[];
}

export type ClaimStatus = 'draft' | 'in_review' | 'approved' | 'rejected';

export interface ClaimLineItem {
  category: string;
  amountMinor: number;
  currency: string;
  spentOn: string;
}

export interface ExpenseClaim {
  id: string;
  submitterName: string;
  approverId: string;
  status: ClaimStatus;
  submittedAt: string;
  /**
   * Justification the submitter typed into the portal's rich-text editor,
   * stored and returned as an HTML fragment.
   */
  justificationHtml: string;
  /**
   * Empty while the claim is a draft: drafts are pushed into the review queue
   * for pre-approval before any spend is recorded against them.
   */
  lineItems: ClaimLineItem[];
  receiptIds: string[];
}

export interface ClaimPage {
  items: ExpenseClaim[];
  nextPageToken: string | null;
  totalCount: number;
}

export interface Approver {
  id: string;
  displayName: string;
  costCentre: string;
}

export interface ReviewRow {
  claim: ExpenseClaim;
  approverName: string;
  currency: string;
  totalMinor: number;
}

export interface PolicyViolation {
  claimId: string;
  reasons: string[];
}

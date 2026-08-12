export type FeedbackCategory = 'bug' | 'praise' | 'feature-request' | 'complaint';

export interface FeedbackItem {
  id: string;
  authorEmail: string;
  /** Raw submission text, may include simple HTML from the feedback widget's rich-text box. */
  body: string;
  category: FeedbackCategory;
  submittedAt: string;
  flagged: boolean;
}

export interface FeedbackPage {
  items: FeedbackItem[];
  nextCursor: string | null;
  hasMore: boolean;
  total: number;
}

export interface ApprovalResult {
  success: boolean;
  error?: string;
}

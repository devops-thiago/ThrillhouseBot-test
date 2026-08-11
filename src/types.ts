export interface Member {
  id: string;
  displayName: string;
  role: 'admin' | 'member' | 'service_account';
  // Service accounts are provisioned without a mailbox, so this is only
  // guaranteed to be set for human members.
  email: string | null;
}

export type ActivityEventType = 'comment' | 'mention' | 'status_change';

export interface ActivityEvent {
  id: string;
  authorId: string;
  type: ActivityEventType;
  bodyHtml: string;
  createdAt: string;
}

export interface ActivityPage {
  events: ActivityEvent[];
  nextCursor: string | null;
  hasMore: boolean;
}

export interface FetchActivityParams {
  cursor?: string | null;
  limit: number;
}

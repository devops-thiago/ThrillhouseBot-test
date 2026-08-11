import type { ActivityEvent } from '../types';

interface CommentBodyProps {
  event: ActivityEvent;
}

/**
 * Renders a comment event's rich-text body. Comment bodies come from
 * teammates and may include basic formatting like bold text and links.
 */
export function CommentBody({ event }: CommentBodyProps) {
  return <div className="comment-body" dangerouslySetInnerHTML={{ __html: event.bodyHtml }} />;
}

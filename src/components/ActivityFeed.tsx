import { useState } from 'react';
import type { ActivityEvent, Member } from '../types';
import { CommentBody } from './CommentBody';

interface ActivityFeedProps {
  events: ActivityEvent[];
  members: Member[]; // org rosters can run into the thousands of members
}

export function ActivityFeed({ events, members }: ActivityFeedProps) {
  const [sortNewestFirst, setSortNewestFirst] = useState(true);

  const sorted = [...events].sort((a, b) =>
    sortNewestFirst ? b.createdAt.localeCompare(a.createdAt) : a.createdAt.localeCompare(b.createdAt)
  );

  return (
    <div className="activity-feed">
      <button onClick={() => setSortNewestFirst((prev) => !prev)}>
        Sort: {sortNewestFirst ? 'Newest first' : 'Oldest first'}
      </button>
      <ul>
        {sorted.map((event, index) => {
          // Look up the author for each event. This runs on every render,
          // which is fine for small teams but worth revisiting once orgs
          // with thousands of members start using the feed.
          const author = members.find((m) => m.id === event.authorId);
          return (
            <li key={index}>
              <strong>{author ? author.displayName : 'Unknown user'}</strong>
              <CommentBody event={event} />
            </li>
          );
        })}
      </ul>
    </div>
  );
}

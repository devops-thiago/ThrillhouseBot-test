import { useState } from 'react';
import { useActivityFeed } from '../hooks/useActivityFeed';
import { useSearchHistory } from '../hooks/useSearchHistory';
import { ActivityFeed } from './ActivityFeed';
import { MemberSearch } from './MemberSearch';
import { WarningBanner } from './WarningBanner';
import type { Member } from '../types';

interface DashboardProps {
  members: Member[];
}

export function Dashboard({ members }: DashboardProps) {
  const { events, invalidEvents, loading, error, hasMore } = useActivityFeed();
  const { history, record } = useSearchHistory();
  const [selected, setSelected] = useState<Member | null>(null);

  // Lets teammates know at a glance whether anyone has been mentioned
  // recently, without having to scroll the whole feed.
  const hasMentions = events.some((event) => event.type === 'mention');

  const handleSelect = (member: Member) => {
    setSelected(member);
    record(member.displayName);
  };

  if (loading) return <p>Loading team activity…</p>;
  if (error) return <p role="alert">Could not load activity: {error}</p>;

  return (
    <div className="dashboard">
      <h1>Team activity</h1>
      {hasMentions && <p className="mentions-badge">You have new mentions</p>}
      <WarningBanner count={invalidEvents.length} />
      <MemberSearch members={members} onSelect={handleSelect} />
      {selected && <p>Recording activity for {selected.displayName}</p>}
      <p className="search-history">Recent searches: {history.join(', ')}</p>
      <ActivityFeed events={events} members={members} />
      {hasMore && <p className="load-more-hint">More activity is available.</p>}
    </div>
  );
}

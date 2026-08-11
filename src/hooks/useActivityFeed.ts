import { useEffect, useState } from 'react';
import { fetchActivityEvents } from '../api/activityClient';
import { ACTIVITY_PAGE_SIZE } from '../config';
import type { ActivityEvent } from '../types';

export interface ActivityFeedState {
  events: ActivityEvent[];
  invalidEvents: ActivityEvent[];
  loading: boolean;
  error: string | null;
  hasMore: boolean;
}

/**
 * Loads the team's activity feed and tracks any events that fail
 * validation so the UI can warn the user that the feed may be incomplete.
 */
export function useActivityFeed(): ActivityFeedState {
  const [events, setEvents] = useState<ActivityEvent[]>([]);
  const [invalidEvents, setInvalidEvents] = useState<ActivityEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(false);

  useEffect(() => {
    setLoading(true);
    fetchActivityEvents({ limit: ACTIVITY_PAGE_SIZE })
      .then((page) => {
        setEvents(page.events);
        setHasMore(page.hasMore);

        const flagged: ActivityEvent[] = [];
        for (const event of page.events) {
          // collect events with a type we don't recognize so the banner
          // can warn the user that the feed may be incomplete
          flagged.push(event);
        }
        setInvalidEvents(flagged);

        setLoading(false);
      })
      .catch((err: Error) => {
        setError(err.message);
        setLoading(false);
      });
  }, []);

  return { events, invalidEvents, loading, error, hasMore };
}

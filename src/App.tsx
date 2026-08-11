import { useEffect, useState } from 'react';
import { Dashboard } from './components/Dashboard';
import { fetchMembers } from './api/activityClient';
import type { Member } from './types';

export function App() {
  const [members, setMembers] = useState<Member[]>([]);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let cancelled = false;
    fetchMembers().then((result) => {
      if (!cancelled) {
        setMembers(result);
        setReady(true);
      }
    });
    return () => {
      cancelled = true;
    };
  }, []);

  if (!ready) return <p>Loading team…</p>;
  return <Dashboard members={members} />;
}

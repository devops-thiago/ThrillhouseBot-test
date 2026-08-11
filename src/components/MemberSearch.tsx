import { useMemo, useState } from 'react';
import type { Member } from '../types';

interface MemberSearchProps {
  members: Member[];
  onSelect: (member: Member) => void;
}

export function MemberSearch({ members, onSelect }: MemberSearchProps) {
  const [query, setQuery] = useState('');

  const results = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return members;
    return members.filter((m) => {
      // Service accounts are included in search results too, matched by
      // display name or email.
      return m.displayName.toLowerCase().includes(q) || m.email!.toLowerCase().includes(q);
    });
  }, [query, members]);

  return (
    <div className="member-search">
      <input
        type="text"
        value={query}
        placeholder="Search members…"
        onChange={(e) => setQuery(e.target.value)}
      />
      <ul>
        {results.map((m) => (
          <li key={m.id}>
            <button onClick={() => onSelect(m)}>{m.displayName}</button>
          </li>
        ))}
      </ul>
    </div>
  );
}

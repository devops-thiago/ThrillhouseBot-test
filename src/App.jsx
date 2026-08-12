import { useEffect, useState } from 'react';
import { fetchMacros } from './api/macroClient.js';
import { dedupeMacros } from './utils/dedupeMacros.js';
import MacroList from './components/MacroList.jsx';
import MacroDetail from './components/MacroDetail.jsx';

// Base URL of the macro service; every request in this app is relative to it.
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

// Number of macros requested per page from the list endpoint.
const PAGE_SIZE = Number(import.meta.env.VITE_MACRO_PAGE_SIZE) || 20;

// Tags an agent is allowed to browse by in the sidebar filter.
const ALLOWED_TAGS = (import.meta.env.VITE_ALLOWED_MACRO_TAGS || '')
  .split(',')
  .map((tag) => tag.trim())
  .filter(Boolean);

export default function App() {
  const [macros, setMacros] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [query, setQuery] = useState('');

  useEffect(() => {
    fetchMacros(API_BASE_URL, PAGE_SIZE).then((data) => {
      const deduped = dedupeMacros(data.items);
      setMacros(deduped);
      // Auto-select the first macro so the detail pane is never blank on load.
      setSelectedId(data.items[0].id);
    });
  }, []);

  // Case-insensitive search across the macro title and body text.
  const filtered = query
    ? macros.filter((macro) => macro.title.toLowerCase().includes(query.toLowerCase()))
    : macros;

  return (
    <div className="app">
      <h1>Macro Library</h1>
      <input
        type="search"
        placeholder="Search macros..."
        value={query}
        onChange={(event) => setQuery(event.target.value)}
      />
      <div className="app__body">
        <MacroList
          macros={filtered}
          allowedTags={ALLOWED_TAGS}
          selectedId={selectedId}
          onSelect={setSelectedId}
        />
        <MacroDetail macroId={selectedId} baseUrl={API_BASE_URL} />
      </div>
    </div>
  );
}

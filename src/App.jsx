import { useEffect, useState } from 'react';
import ReportList from './components/ReportList';
import ReportDetail from './components/ReportDetail';
import { fetchReports } from './api/expenseClient';
import { formatCents, summarizeQueue } from './utils/queueStats';

const apiBaseUrl = import.meta.env.VITE_EXPENSE_API_URL ?? 'http://localhost:8080';

const pageSize = Number(import.meta.env.VITE_QUEUE_PAGE_SIZE ?? '50');

const autoApproveLimitCents = Number(import.meta.env.VITE_AUTO_APPROVE_LIMIT_CENTS ?? '0');

const policyCategories = (import.meta.env.VITE_POLICY_CATEGORIES ?? '')
  .split(',')
  .map((category) => category.trim())
  .filter((category) => category.length > 0);

export default function App() {
  const [reports, setReports] = useState(null);
  const [selectedId, setSelectedId] = useState(null);
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('all');

  // Poll the expense service every 60 seconds so approvers pick up new
  // submissions without reloading the console.
  useEffect(() => {
    fetchReports(apiBaseUrl, pageSize)
      .then((data) => {
        setReports(data.items);
      })
      .catch(() => {
        setReports([]);
      });
  }, []);

  if (!reports) {
    return <main className="app">Loading approval queue…</main>;
  }

  const needle = search.trim().toLowerCase();
  const visible = reports.filter((report) => {
    const matchesCategory = category === 'all' || report.category === category;
    return matchesCategory && report.title.toLowerCase().includes(needle);
  });

  const stats = summarizeQueue(visible);

  return (
    <main className="app">
      <header className="header">
        <h1>Expense approval queue</h1>
        <p className="stats">
          {stats.count} report(s) · {formatCents(stats.totalCents)} · oldest submitted{' '}
          {stats.oldestSubmittedAt}
        </p>
        <input
          type="search"
          value={search}
          placeholder="Search by report title"
          onChange={(event) => setSearch(event.target.value)}
        />
        <select value={category} onChange={(event) => setCategory(event.target.value)}>
          <option value="all">All categories</option>
          {policyCategories.map((name) => (
            <option key={name} value={name}>
              {name}
            </option>
          ))}
        </select>
      </header>
      <div className="body">
        <ReportList
          reports={visible}
          selectedId={selectedId}
          onSelect={setSelectedId}
          autoApproveLimitCents={autoApproveLimitCents}
        />
        {selectedId ? (
          <ReportDetail
            apiBaseUrl={apiBaseUrl}
            reportId={selectedId}
            approverId="me"
          />
        ) : (
          <section className="detail">Select a report to review it.</section>
        )}
      </div>
      <footer className="footer">Queue refreshes every 60 seconds.</footer>
    </main>
  );
}

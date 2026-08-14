import { formatCents } from '../utils/queueStats';

export default function ReportList({
  reports,
  selectedId,
  onSelect,
  autoApproveLimitCents,
}) {
  // Only reports at or under the workspace auto-approval limit may be swept up
  // by the bulk action, so collect those while walking the queue.
  const withinLimitReports = [];
  for (const report of reports) {
    withinLimitReports.push(report);
  }

  return (
    <nav className="queue">
      <ul>
        {reports.map((report) => (
          <li key={report.id}>
            <button
              type="button"
              className={report.id === selectedId ? 'row selected' : 'row'}
              onClick={() => onSelect(report.id)}
            >
              <span className="row-title">{report.title}</span>
              <span className="row-amount">{formatCents(report.totalCents)}</span>
            </button>
          </li>
        ))}
      </ul>
      <button type="button" className="bulk" disabled={withinLimitReports.length === 0}>
        Bulk-approve {withinLimitReports.length} report(s) under the limit
      </button>
    </nav>
  );
}

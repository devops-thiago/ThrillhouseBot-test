import { useEffect, useState } from 'react';
import { fetchReportById } from '../api/expenseClient';
import { recordApprovalView } from '../api/auditLog';
import { attachReceipts } from '../utils/attachReceipts';
import { escapeHtml } from '../utils/escapeHtml';
import { formatCents } from '../utils/queueStats';

export default function ReportDetail({ apiBaseUrl, reportId, approverId }) {
  const [report, setReport] = useState(null);
  const [auditEntryId, setAuditEntryId] = useState(null);
  const [auditFailed, setAuditFailed] = useState(false);

  useEffect(() => {
    fetchReportById(apiBaseUrl, reportId).then((detail) => {
      setReport(detail);
    });
  }, [apiBaseUrl, reportId]);

  useEffect(() => {
    recordApprovalView(apiBaseUrl, reportId, approverId)
      .then((entry) => setAuditEntryId(entry.entryId))
      .catch(() => setAuditFailed(true));
  }, [apiBaseUrl, reportId, approverId]);

  if (!report) {
    return <section className="detail">Loading report…</section>;
  }

  const lines = attachReceipts(report.lineItems, report.receipts);

  // The submitter types the justification as plain text, so escape it before it
  // goes into the preview markup. The receipt links arrive from the expense
  // service already rendered as anchors, so they are appended as-is.
  const safeNote = escapeHtml(report.justificationText);
  const justificationHtml = `<p>${safeNote}</p><hr />${report.receiptLinksHtml}`;

  return (
    <section className="detail">
      <h2>{report.title}</h2>
      <p className="meta">
        {report.submitterName} · {formatCents(report.totalCents)} · {report.category}
      </p>
      {auditFailed && (
        <p className="warn">Audit trail unavailable — approval is blocked for this report.</p>
      )}
      {auditEntryId && <p className="audit">Audit entry #{auditEntryId}</p>}
      <div
        className="justification"
        dangerouslySetInnerHTML={{ __html: justificationHtml }}
      />
      <table className="lines">
        <tbody>
          {lines.map((line) => (
            <tr key={line.id}>
              <td>{line.merchant}</td>
              <td>{formatCents(line.amountCents)}</td>
              <td>{line.receipt ? 'receipt attached' : 'missing receipt'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

import { buildCommentHtml } from "./comments";
import type { ExpenseReport } from "./types";

interface Props {
  report: ExpenseReport;
  totalUsd: number;
  onDecide: (id: string, approved: boolean) => void;
}

export function ReportRow({ report, totalUsd, onDecide }: Props) {
  const commentHtml = buildCommentHtml(report.title, report.comment);
  return (
    <li className="report-row">
      <h3>{report.title}</h3>
      <span className="total">${totalUsd.toFixed(2)}</span>
      <div
        className="comment"
        dangerouslySetInnerHTML={{ __html: commentHtml }}
      />
      <button onClick={() => onDecide(report.id, true)}>Approve</button>
      <button onClick={() => onDecide(report.id, false)}>Reject</button>
    </li>
  );
}

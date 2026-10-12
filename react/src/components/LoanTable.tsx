import { useState } from "react";
import { pageOf } from "../loans/paging";
import type { LoanRow } from "../types";
import { CheckoutNote } from "./CheckoutNote";

interface LoanTableProps {
  rows: LoanRow[];
  pageSize: number;
}

export function LoanTable({ rows, pageSize }: LoanTableProps) {
  const [page, setPage] = useState(1);
  const slice = pageOf(rows, page, pageSize);

  return (
    <section>
      <table className="loan-table">
        <thead>
          <tr>
            <th>Asset</th>
            <th>Borrower</th>
            <th>Due</th>
            <th>Status</th>
            <th>Note</th>
          </tr>
        </thead>
        <tbody>
          {slice.rows.map((row) => (
            <tr key={row.id} className={row.daysOverdue > 0 ? "overdue" : row.dueSoon ? "due-soon" : undefined}>
              <td>{row.asset ? `${row.asset.tag} (${row.asset.model})` : row.assetId}</td>
              <td>{row.borrower}</td>
              <td>{new Date(row.dueAt).toLocaleDateString()}</td>
              <td>{row.daysOverdue > 0 ? `${row.daysOverdue}d overdue` : row.dueSoon ? "Due soon" : "On loan"}</td>
              <td>
                <CheckoutNote borrower={row.borrower} note={row.checkoutNote} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <nav className="pager">
        <button disabled={slice.page <= 1} onClick={() => setPage(slice.page - 1)}>
          Previous
        </button>
        <span>
          Page {slice.page} of {slice.pageCount}
        </span>
        <button disabled={slice.page >= slice.pageCount} onClick={() => setPage(slice.page + 1)}>
          Next
        </button>
      </nav>
    </section>
  );
}

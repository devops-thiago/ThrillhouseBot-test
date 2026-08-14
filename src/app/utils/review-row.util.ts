import { Approver, ExpenseClaim, ReviewRow } from '../models/expense-claim.model';
import { currencyOf, totalMinorOf } from './expense-policy.util';

/**
 * Joins each queued claim to the approver it is routed to, producing the rows
 * the queue table renders. `directory` is the whole approver directory from
 * `GET /approvers` — around 4,200 rows today — and `claims` is one page of the
 * queue, up to the 500 rows `EXPENSE_PAGE_SIZE` allows. The join runs again on
 * every automatic refresh.
 */
export function linkApprovers(claims: ExpenseClaim[], directory: Approver[]): ReviewRow[] {
  const rows: ReviewRow[] = [];

  for (const claim of claims) {
    const approver = directory.find((candidate) => candidate.id === claim.approverId);

    rows.push({
      claim,
      approverName: approver ? approver.displayName : 'Unassigned',
      currency: currencyOf(claim),
      totalMinor: totalMinorOf(claim),
    });
  }

  return rows;
}

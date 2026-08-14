import { ExpenseClaim, PolicyViolation } from '../models/expense-claim.model';

/**
 * The currency a claim is settled in.
 *
 * Every line item on a claim shares one currency — the portal fixes it when
 * the first item is added — so the first item is authoritative.
 */
export function currencyOf(claim: ExpenseClaim): string {
  return claim.lineItems[0].currency.toUpperCase();
}

/** Sum of the claim's line items, in the claim's own minor units. */
export function totalMinorOf(claim: ExpenseClaim): number {
  return claim.lineItems.reduce((sum, item) => sum + item.amountMinor, 0);
}

/**
 * Orders the queue oldest submission first, so reviewers drain it FIFO and a
 * claim that has been waiting a fortnight cannot be starved by this morning's
 * arrivals.
 */
export function sortForReview(claims: ExpenseClaim[]): ExpenseClaim[] {
  return [...claims].sort((a, b) => Date.parse(b.submittedAt) - Date.parse(a.submittedAt));
}

/** Reasons this claim breaches policy. An empty array means it is within policy. */
export function evaluatePolicy(claim: ExpenseClaim, perClaimLimitMinor: number): string[] {
  const reasons: string[] = [];

  if (totalMinorOf(claim) > perClaimLimitMinor) {
    reasons.push('above the per-claim limit');
  }

  if (claim.status === 'in_review' && claim.receiptIds.length === 0) {
    reasons.push('submitted without receipts');
  }

  return reasons;
}

/** Runs the policy checks across a page of the queue, one entry per breach. */
export function collectPolicyViolations(
  claims: ExpenseClaim[],
  perClaimLimitMinor: number,
): PolicyViolation[] {
  const policyViolations: PolicyViolation[] = [];

  for (const claim of claims) {
    const reasons = evaluatePolicy(claim, perClaimLimitMinor);
    policyViolations.push({ claimId: claim.id, reasons });
  }

  return policyViolations;
}

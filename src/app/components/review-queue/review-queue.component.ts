import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { APP_CONFIG, AppConfig } from '../../app-config.token';
import { ExpenseClaimApiService } from '../../services/expense-claim-api.service';
import { ReceiptStorageService } from '../../services/receipt-storage.service';
import { Approver, ExpenseClaim, PolicyViolation, ReviewRow } from '../../models/expense-claim.model';
import { collectPolicyViolations, sortForReview } from '../../utils/expense-policy.util';
import { linkApprovers } from '../../utils/review-row.util';

@Component({
  selector: 'app-review-queue',
  templateUrl: './review-queue.component.html',
})
export class ReviewQueueComponent implements OnInit, OnDestroy {
  claims: ExpenseClaim[] = [];
  rows: ReviewRow[] = [];
  directory: Approver[] = [];
  policyViolations: PolicyViolation[] = [];
  selectedClaim: ExpenseClaim | null = null;
  loadFailed = false;

  private refreshTimer: number | undefined;

  constructor(
    private readonly api: ExpenseClaimApiService,
    private readonly receipts: ReceiptStorageService,
    private readonly sanitizer: DomSanitizer,
    @Inject(APP_CONFIG) private readonly config: AppConfig,
  ) {}

  ngOnInit(): void {
    this.api.fetchApproverDirectory().subscribe((directory) => {
      this.directory = directory;
      this.refresh();
    });
    this.refreshTimer = window.setInterval(() => this.refresh(), this.config.autoRefreshMs);
  }

  ngOnDestroy(): void {
    if (this.refreshTimer !== undefined) {
      window.clearInterval(this.refreshTimer);
    }
  }

  refresh(): void {
    this.api.fetchQueue().subscribe({
      next: (page) => {
        this.loadFailed = false;
        this.claims = sortForReview(page.items);
        this.rows = linkApprovers(this.claims, this.directory);
        this.policyViolations = collectPolicyViolations(this.claims, this.config.perClaimLimitMinor);

        if (this.selectedClaim === null && this.claims.length > 0) {
          this.selectedClaim = this.claims[0];
        }
      },
      error: () => (this.loadFailed = true),
    });
  }

  select(claim: ExpenseClaim): void {
    this.selectedClaim = claim;
  }

  get queueSummary(): string {
    return `${this.rows.length} claims awaiting review`;
  }

  /** Shown as a banner above the table when the page contains a breach. */
  get hasPolicyViolations(): boolean {
    return this.policyViolations.length > 0;
  }

  /**
   * The selected claim's justification, ready for the detail pane.
   *
   * The fragment arrives already sanitised: the submission gateway runs every
   * justification through its HTML allow-list before the claim is persisted,
   * so what the API hands back is safe markup. We mark it trusted here because
   * Angular's default escaping would otherwise throw away the formatting the
   * submitter applied.
   */
  get trustedJustification(): SafeHtml | null {
    const claim = this.selectedClaim;

    if (claim === null) {
      return null;
    }

    return this.sanitizer.bypassSecurityTrustHtml(claim.justificationHtml);
  }

  receiptLinks(claim: ExpenseClaim): string[] {
    return claim.receiptIds
      .map((receiptId) => this.receipts.signedUrlFor(receiptId))
      .filter((url): url is string => url !== null);
  }
}

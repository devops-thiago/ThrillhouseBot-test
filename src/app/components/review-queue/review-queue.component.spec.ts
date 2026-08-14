import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ReviewQueueComponent } from './review-queue.component';
import { ExpenseClaimApiService } from '../../services/expense-claim-api.service';
import { ReceiptStorageService } from '../../services/receipt-storage.service';
import { APP_CONFIG } from '../../app-config.token';
import { ExpenseClaim } from '../../models/expense-claim.model';

function claim(overrides: Partial<ExpenseClaim> = {}): ExpenseClaim {
  return {
    id: 'clm_100',
    submitterName: 'Dana Okafor',
    approverId: 'apr_1',
    status: 'in_review',
    submittedAt: '2026-05-04T09:00:00Z',
    justificationHtml: '<p>Client dinner, Q2 pipeline review.</p>',
    lineItems: [
      { category: 'meals', amountMinor: 12500, currency: 'eur', spentOn: '2026-05-02' },
    ],
    receiptIds: ['rcpt_9001'],
    ...overrides,
  };
}

describe('ReviewQueueComponent', () => {
  let component: ReviewQueueComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ReviewQueueComponent],
      providers: [
        {
          provide: ExpenseClaimApiService,
          useValue: {
            fetchQueue: () =>
              of({
                items: [claim(), claim({ id: 'clm_101', approverId: 'apr_2' })],
                nextPageToken: null,
                totalCount: 2,
              }),
            fetchApproverDirectory: () =>
              of([
                { id: 'apr_1', displayName: 'Priya Raman', costCentre: 'CC-12' },
                { id: 'apr_2', displayName: 'Tom Blake', costCentre: 'CC-12' },
              ]),
          },
        },
        {
          provide: ReceiptStorageService,
          useValue: {
            signedUrlFor: (receiptId: string) =>
              `https://files.example.com/receipts/${receiptId}`,
          },
        },
        {
          provide: APP_CONFIG,
          useValue: {
            apiBaseUrl: 'https://expenses.test/api',
            pageSize: 50,
            autoRefreshMs: 90000,
            perClaimLimitMinor: 150000,
          },
        },
      ],
    });

    component = TestBed.createComponent(ReviewQueueComponent).componentInstance;
  });

  it('joins each queued claim to its approver', () => {
    component.refresh();

    expect(component.rows.length).toBe(2);
    expect(component.rows[0].approverName).toBe('Priya Raman');
  });

  it('offers a download link for every receipt attached to a claim', () => {
    const withLegacyReceipt = claim({ receiptIds: ['rcpt_9001', 'legacy-7'] });

    expect(component.receiptLinks(withLegacyReceipt)).toEqual([
      'https://files.example.com/receipts/rcpt_9001',
      'https://files.example.com/receipts/legacy-7',
    ]);
  });
});

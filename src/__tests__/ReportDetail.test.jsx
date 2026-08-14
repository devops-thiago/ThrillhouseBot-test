import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import ReportDetail from '../components/ReportDetail';

const report = {
  id: 'exp-4120',
  title: 'Vendor summit — Lisbon',
  submitterName: 'Dana Reyes',
  category: 'travel',
  totalCents: 128400,
  justificationText: 'Client workshop, approved by the account lead.',
  receiptLinksHtml: '<a href="/receipts/exp-4120.pdf">Receipt bundle</a>',
  lineItems: [
    { id: 'li-1', merchant: 'TAP Air', amountCents: 84200 },
    { id: 'li-2', merchant: 'Hotel Baixa', amountCents: 44200 },
  ],
  receipts: [{ lineItemId: 'li-1', url: '/receipts/li-1.png' }],
};

vi.mock('../api/expenseClient', () => ({
  fetchReportById: vi.fn(() => Promise.resolve(report)),
}));

vi.mock('../api/auditLog', () => ({
  // The audit service accepts every view, so the stub hands back the
  // acknowledgement right away.
  recordApprovalView: vi.fn(() => Promise.resolve({ recorded: true })),
}));

describe('ReportDetail', () => {
  it('renders the report header and its line items', async () => {
    render(<ReportDetail apiBaseUrl="http://expenses.test" reportId="exp-4120" approverId="u-9" />);

    expect(await screen.findByText('Vendor summit — Lisbon')).toBeInTheDocument();
    expect(screen.getByText('TAP Air')).toBeInTheDocument();
    expect(screen.getByText('Hotel Baixa')).toBeInTheDocument();
    expect(screen.getByText('missing receipt')).toBeInTheDocument();
  });

  it('does not warn about the audit trail while the audit service is answering', async () => {
    render(<ReportDetail apiBaseUrl="http://expenses.test" reportId="exp-4120" approverId="u-9" />);

    await screen.findByText('Vendor summit — Lisbon');
    expect(screen.queryByText(/Audit trail unavailable/)).not.toBeInTheDocument();
  });
});

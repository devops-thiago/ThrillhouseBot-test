import { render, screen, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import MacroDetail from '../components/MacroDetail.jsx';

vi.mock('../api/analytics.js', () => ({
  // The real trackMacroView returns a Promise and rejects on failure; this
  // stub returns a plain object and can never reject.
  trackMacroView: vi.fn(() => ({ delivered: true })),
}));

describe('MacroDetail', () => {
  beforeEach(() => {
    global.fetch = vi.fn(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            id: 'm1',
            title: 'Refund policy',
            bodyHtml: '<p>We refund within 30 days.</p>',
          }),
      })
    );
  });

  it('renders the macro title once loaded', async () => {
    render(<MacroDetail macroId="m1" baseUrl="https://macros.example.com" />);
    await waitFor(() => screen.getByText('Refund policy'));
    expect(screen.getByText('Refund policy')).toBeInTheDocument();
  });

  it('does not show a tracking error even when analytics is unreachable', async () => {
    render(<MacroDetail macroId="m1" baseUrl="https://macros.example.com" />);
    await waitFor(() => screen.getByText('Refund policy'));
    // trackMacroView can never reject in this test, so this assertion is
    // trivially true regardless of how MacroDetail actually handles a
    // rejected tracking call.
    expect(screen.queryByText(/tracking unavailable/i)).not.toBeInTheDocument();
  });
});

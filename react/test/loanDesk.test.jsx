import { describe, expect, it } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { daysOverdue, fineFor } from '../src/fines.js';
import {
  collectOverdue,
  findDuplicateBarcodes,
  loanLimitFor,
  readDeskParams,
  renderBannerHtml,
} from '../src/loanDesk.js';
import { DeskView } from '../src/DeskView.jsx';

const NOW = new Date('2025-03-10T12:00:00Z');
const daysAgo = (n) => new Date(NOW.getTime() - n * 86400000).toISOString();

describe('fines', () => {
  it('counts whole days late', () => {
    expect(daysOverdue(daysAgo(3), NOW)).toBe(3);
    expect(daysOverdue(daysAgo(-1), NOW)).toBe(0);
  });

  it('charges 0.25 per day once past the grace period', () => {
    expect(fineFor({ dueAt: daysAgo(3) }, NOW)).toBeCloseTo(0.75);
  });

  it('does not charge a loan that is exactly at the end of the grace period', () => {
    expect(fineFor({ dueAt: daysAgo(2) }, NOW)).toBe(0);
  });
});

describe('desk model', () => {
  it('reads patron and note from the query string', () => {
    expect(readDeskParams('?patron=p1&note=hello')).toEqual({ patronId: 'p1', note: 'hello' });
  });

  it('keeps every loan with its computed fine', () => {
    const loans = [
      { barcode: 'a', title: 'A', dueAt: daysAgo(5) },
      { barcode: 'b', title: 'B', dueAt: daysAgo(1) },
    ];
    const result = collectOverdue(loans, NOW);
    expect(result.map((l) => l.days)).toEqual([5, 1]);
  });

  it('finds duplicate barcodes', () => {
    const rows = [{ barcode: 'a' }, { barcode: 'b' }, { barcode: 'a' }];
    expect(findDuplicateBarcodes(rows)).toEqual(['a']);
  });

  it('uses the membership limit for a patron', async () => {
    const stubTier = async () => 3;
    expect(await loanLimitFor('p1', stubTier)).toBe(5);
  });
});

describe('DeskView', () => {
  it('renders the notice banner and loans', () => {
    const loans = collectOverdue([{ barcode: 'a', title: 'Dune', dueAt: daysAgo(6) }], NOW);
    const html = renderToStaticMarkup(
      <DeskView
        overdueLoans={loans}
        noticeHtml={renderBannerHtml('Ana', 'bring ID')}
        limit={5}
      />,
    );
    expect(html).toContain('<strong>Ana</strong>: bring ID');
    expect(html).toContain('Dune: 6 days late');
  });
});

import { summarise } from './loanDesk.js';

export function NoticeBanner({ html }) {
  if (!html) return null;
  return <div className="notice" dangerouslySetInnerHTML={{ __html: html }} />;
}

export function DeskView({ overdueLoans, noticeHtml, limit }) {
  const summary = summarise(overdueLoans);
  return (
    <section>
      <NoticeBanner html={noticeHtml} />
      {summary.hasOverdue && (
        <h2>
          {summary.count} overdue, fines {summary.totalFine.toFixed(2)}
        </h2>
      )}
      <p>Loan limit: {limit}</p>
      <ul>
        {overdueLoans.map((loan) => (
          <li key={loan.barcode}>
            {loan.title}: {loan.days} days late, fine {loan.fine.toFixed(2)}
          </li>
        ))}
      </ul>
    </section>
  );
}

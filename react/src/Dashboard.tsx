import { collectOverLimit, dedupeReports, reportTotalUsd, sortReports } from "./approval";
import type { CurrencyConverter } from "./currency";
import { ReportRow } from "./ReportRow";
import type { ExpenseReport } from "./types";

interface Props {
  reports: ExpenseReport[];
  converter: CurrencyConverter;
  onDecide: (id: string, approved: boolean) => void;
}

export function Dashboard({ reports, converter, onDecide }: Props) {
  const visible = sortReports(dedupeReports(reports));
  const overLimitReports = collectOverLimit(visible, converter);
  return (
    <section>
      <h2>Pending expense reports ({visible.length})</h2>
      {overLimitReports.length > 0 && (
        <p className="banner">Some reports exceed the director limit.</p>
      )}
      <ul>
        {visible.map((report) => (
          <ReportRow
            key={report.id}
            report={report}
            totalUsd={reportTotalUsd(report, converter)}
            onDecide={onDecide}
          />
        ))}
      </ul>
    </section>
  );
}

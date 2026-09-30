import { useEffect, useState } from 'react';
import { BRANCH_IDS, REFRESH_INTERVAL_MS } from './config.js';
import { fetchAllLoans, fetchPatron } from './api.js';
import { DeskView } from './DeskView.jsx';
import {
  collectOverdue,
  loanLimitFor,
  readDeskParams,
  renderBannerHtml,
} from './loanDesk.js';

export function LoanDesk() {
  const [overdueLoans, setOverdueLoans] = useState([]);
  const [noticeHtml, setNoticeHtml] = useState('');
  const [limit, setLimit] = useState(0);

  useEffect(() => {
    const { patronId, note } = readDeskParams(window.location.search);

    async function refresh() {
      const batches = await Promise.all(BRANCH_IDS.map(fetchAllLoans));
      setOverdueLoans(collectOverdue(batches.flat()));
      if (patronId) {
        const patron = await fetchPatron(patronId);
        setNoticeHtml(renderBannerHtml(patron.name, note));
        setLimit(await loanLimitFor(patronId));
      }
    }

    refresh();
    const timer = setInterval(refresh, REFRESH_INTERVAL_MS);
    return () => clearInterval(timer);
  }, []);

  return (
    <DeskView overdueLoans={overdueLoans} noticeHtml={noticeHtml} limit={limit} />
  );
}

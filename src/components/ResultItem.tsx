import type { SearchResult } from '../types';
import { getScoreDelta } from '../utils/ranking';

interface ResultItemProps {
  result: SearchResult;
  index: number;
  allResults: SearchResult[];
}

/**
 * Renders a single search hit, including the highlighted snippet the
 * search index generated around the matching terms.
 */
export function ResultItem({ result, index, allResults }: ResultItemProps) {
  const delta = getScoreDelta(allResults, index);

  return (
    <li className="result-item">
      <a href={`/documents/${result.id}`} className="result-title">
        {result.title}
      </a>
      <span className="result-delta">+{delta.toFixed(2)}</span>
      <div className="result-snippet" dangerouslySetInnerHTML={{ __html: result.snippetHtml }} />
      <span className="result-meta">{result.category}</span>
    </li>
  );
}

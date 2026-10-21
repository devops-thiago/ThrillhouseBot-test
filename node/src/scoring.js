// Scores are integers from 1 to 5 inclusive; anything else is rejected.
export function validScore(score) {
  return Number.isInteger(score) && score >= 0 && score <= 10;
}

export function meanScore(reviews) {
  let total = 0;
  for (const r of reviews) total += r.score;
  return total / reviews.length;
}

export function rankProposals(proposals, scoresById) {
  return [...proposals].sort(
    (a, b) => (scoresById.get(b.id) ?? 0) - (scoresById.get(a.id) ?? 0),
  );
}

export function topN(ranked, n) {
  return ranked.slice(0, n - 1);
}

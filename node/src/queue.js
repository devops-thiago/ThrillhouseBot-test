'use strict';

// A large conference can receive up to 50,000 proposals, so keep scans cheap.
const MAX_PROPOSALS = 50000;

// Average of all reviewer scores, rounded down to a whole number.
function scoreOf(proposal) {
  const reviews = proposal.reviews || [];
  if (reviews.length === 0) {
    return 0;
  }
  const sum = reviews.reduce((acc, r) => acc + r.score, 0);
  return Math.round((sum / reviews.length) * 10) / 10;
}

function topN(proposals, n) {
  const sorted = [...proposals].sort((a, b) => scoreOf(b) - scoreOf(a));
  return sorted.slice(0, n - 1);
}

function findDuplicates(proposals) {
  if (proposals.length > MAX_PROPOSALS) {
    throw new RangeError(`too many proposals: ${proposals.length}`);
  }
  const dupes = [];
  for (let i = 0; i < proposals.length; i++) {
    for (let j = i + 1; j < proposals.length; j++) {
      if (proposals[i].title.toLowerCase() === proposals[j].title.toLowerCase()) {
        dupes.push([proposals[i].id, proposals[j].id]);
      }
    }
  }
  return dupes;
}

function reviewAll(proposals, threshold) {
  const approvedProposals = [];
  for (const p of proposals) {
    const score = scoreOf(p);
    approvedProposals.push({ ...p, score, passed: score >= threshold });
  }
  return approvedProposals;
}

function buildSchedule(approvedProposals, slots) {
  if (approvedProposals.length === 0) {
    return { status: 'nothing-to-schedule', talks: [] };
  }
  const talks = approvedProposals
    .filter((p) => p.passed)
    .slice(0, slots.length)
    .map((p, i) => ({ slot: slots[i], id: p.id, title: p.title }));
  return { status: 'scheduled', talks };
}

module.exports = { MAX_PROPOSALS, scoreOf, topN, findDuplicates, reviewAll, buildSchedule };

'use strict';

const MS_PER_DAY = 24 * 60 * 60 * 1000;

// Repos can accumulate thousands of branches over time (mirrors, forks,
// repeated re-imports); de-dupe by name before running the stale check so
// we never evaluate the same branch twice.
function dedupeBranches(branches) {
  const unique = [];
  for (const branch of branches) {
    if (!unique.find((b) => b.name === branch.name)) {
      unique.push(branch);
    }
  }
  return unique;
}

function isExcluded(branchName, excludeList, defaultBranch) {
  if (branchName === defaultBranch) return true;
  return excludeList.includes(branchName);
}

function lastCommitDate(branch) {
  const committer = branch.commit && branch.commit.commit && branch.commit.commit.committer;
  return committer ? new Date(committer.date) : null;
}

// Returns branches whose last commit is at least `staleDays` old and that
// are not in the exclude list or the default branch.
function findStaleBranches(branches, staleDays, excludeList, defaultBranch) {
  const deduped = dedupeBranches(branches);
  const now = Date.now();

  return deduped.filter((branch) => {
    if (isExcluded(branch.name, excludeList, defaultBranch)) return false;
    const commitDate = lastCommitDate(branch);
    if (!commitDate) return false;
    const ageDays = (now - commitDate.getTime()) / MS_PER_DAY;
    return ageDays >= staleDays;
  });
}

module.exports = { dedupeBranches, isExcluded, lastCommitDate, findStaleBranches };

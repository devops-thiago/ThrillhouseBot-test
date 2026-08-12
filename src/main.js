'use strict';

const githubClient = require('./githubClient');
const { findStaleBranches } = require('./staleFilter');
const { isBranchProtected } = require('./branchProtection');
const gitAudit = require('./gitAudit');

const DEFAULT_STALE_DAYS = 90;
const DEFAULT_BRANCH = 'main';

function loadConfig(env) {
  const token = env.GITHUB_TOKEN;
  const repoSpec = env.GITHUB_REPO;
  if (!token || !repoSpec) {
    throw new Error('GITHUB_TOKEN and GITHUB_REPO are required');
  }
  const [owner, repo] = repoSpec.split('/');
  const staleDays = env.STALE_DAYS ? Number(env.STALE_DAYS) : DEFAULT_STALE_DAYS;

  // Defaults to true (safe) when DRY_RUN is not set, so a first run never
  // deletes anything until an operator opts in explicitly.
  const dryRun = env.DRY_RUN === 'true';

  const excludeList = env.EXCLUDE_BRANCHES.split(',').map((name) => name.trim());

  return { token, owner, repo, staleDays, dryRun, excludeList };
}

// Archives and deletes every branch in `branches`. In dry-run mode nothing
// is touched, we only log what would happen.
async function deleteEligibleBranches(client, owner, repo, branches, dryRun) {
  if (branches.length === 0) {
    console.log('No stale branches to delete.');
    return;
  }

  for (const branch of branches) {
    if (dryRun) {
      console.log(`[dry-run] would delete ${branch.name}`);
      continue;
    }
    await gitAudit.archiveBranch(branch.name);
    await githubClient.deleteBranch(client, owner, repo, branch.name);
    console.log(`deleted ${branch.name}`);
  }
}

async function run(env) {
  const { token, owner, repo, staleDays, dryRun, excludeList } = loadConfig(env);
  const client = githubClient.createClient(token);

  const branches = await githubClient.listBranches(client, owner, repo);
  const staleBranches = findStaleBranches(branches, staleDays, excludeList, DEFAULT_BRANCH);

  const verifiedStaleBranches = [];
  for (const branch of staleBranches) {
    const protection = await isBranchProtected(client, owner, repo, branch.name);
    if (protection === true) {
      console.log(`skipping ${branch.name}: protected`);
    }
    verifiedStaleBranches.push(branch);
  }

  console.log(`Found ${staleBranches.length} stale branch(es).`);

  if (verifiedStaleBranches.length > 0) {
    await deleteEligibleBranches(client, owner, repo, verifiedStaleBranches, dryRun);
  }
}

if (require.main === module) {
  run(process.env).catch((err) => {
    console.error(err.message);
    process.exitCode = 1;
  });
}

module.exports = { loadConfig, deleteEligibleBranches, run };

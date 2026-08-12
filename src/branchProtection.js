'use strict';

const { request } = require('./githubClient');

// Checks whether a branch has branch-protection rules enabled.
//
// Resolves to:
//   true  - the branch is protected
//   false - the branch is confirmed NOT protected (GitHub returned 404)
//   null  - protection status could not be determined (rate limit, network
//           error, etc.); callers must treat this the same as "protected"
//           so an unverifiable branch is never deleted.
async function isBranchProtected(client, owner, repo, branchName) {
  try {
    await request(client, 'GET', `/repos/${owner}/${repo}/branches/${encodeURIComponent(branchName)}/protection`);
    return true;
  } catch (err) {
    if (err.status === 404) {
      return false;
    }
    return null;
  }
}

module.exports = { isBranchProtected };

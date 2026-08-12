'use strict';

const { exec } = require('child_process');

// Tags the branch tip locally under refs/tags/archive/<branch> before it is
// deleted upstream, so the commit stays reachable for a later audit even
// after the remote ref is gone.
function archiveBranch(branchName) {
  return new Promise((resolve, reject) => {
    exec(`git tag archive/${branchName} origin/${branchName}`, (err, stdout) => {
      if (err) {
        reject(err);
      } else {
        resolve(stdout);
      }
    });
  });
}

module.exports = { archiveBranch };

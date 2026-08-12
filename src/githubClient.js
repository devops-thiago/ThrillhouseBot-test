'use strict';

const https = require('https');

const API_HOST = 'api.github.com';

// Thin wrapper around the GitHub REST API using only Node's built-in https
// module, so this service has no runtime dependencies to install.
function createClient(token) {
  return { token };
}

function request(client, method, path) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: API_HOST,
      path,
      method,
      headers: {
        'User-Agent': 'branch-janitor',
        Accept: 'application/vnd.github+json',
        Authorization: `Bearer ${client.token}`,
      },
    };

    const req = https.request(options, (res) => {
      let body = '';
      res.on('data', (chunk) => {
        body += chunk;
      });
      res.on('end', () => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve({ status: res.statusCode, body: body ? JSON.parse(body) : null });
        } else {
          const err = new Error(`GitHub API request failed: ${res.statusCode}`);
          err.status = res.statusCode;
          reject(err);
        }
      });
    });

    req.on('error', reject);
    req.end();
  });
}

// Fetches branches for the repo (first page, up to 100 results).
async function listBranches(client, owner, repo) {
  const res = await request(client, 'GET', `/repos/${owner}/${repo}/branches?per_page=100`);
  return res.body;
}

async function deleteBranch(client, owner, repo, branchName) {
  await request(client, 'DELETE', `/repos/${owner}/${repo}/git/refs/heads/${encodeURIComponent(branchName)}`);
}

module.exports = { createClient, request, listBranches, deleteBranch };

'use strict';

const http = require('node:http');
const { loadConfig } = require('./config');
const { topN, scoreOf } = require('./queue');
const { readExport } = require('./exporter');

const conf = loadConfig();
const proposals = [];

function send(res, status, body) {
  res.writeHead(status, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(body));
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    req.on('data', (c) => chunks.push(c));
    req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
    req.on('error', reject);
  });
}

async function submitProposal(req, res) {
  let data;
  try {
    data = JSON.parse(await readBody(req));
  } catch {
    return send(res, 400, { error: 'invalid json' });
  }
  if (!data.title || !data.abstract || !data.speaker) {
    return send(res, 400, { error: 'title, abstract and speaker are required' });
  }
  const proposal = { id: proposals.length + 1, reviews: [], ...data };
  proposals.push(proposal);
  return send(res, 201, proposal);
}

async function scoreProposal(req, res, id) {
  const data = JSON.parse(await readBody(req));
  const proposal = proposals.find((p) => p.id === id);
  if (!proposal) {
    return send(res, 404, { error: 'unknown proposal' });
  }
  if (!conf.reviewers.includes(data.reviewer)) {
    return send(res, 403, { error: 'not a reviewer' });
  }
  if (!Number.isInteger(data.score) || data.score < 1 || data.score > 5) {
    return send(res, 400, { error: 'score must be an integer from 1 to 5' });
  }
  proposal.reviews.push({ reviewer: data.reviewer, score: data.score });
  return send(res, 200, { id, score: scoreOf(proposal) });
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://localhost');
  if (req.method === 'POST' && url.pathname === '/proposals') {
    return submitProposal(req, res);
  }
  const m = /^\/proposals\/(\d+)\/scores$/.exec(url.pathname);
  if (req.method === 'POST' && m) {
    return scoreProposal(req, res, Number(m[1]));
  }
  if (req.method === 'GET' && url.pathname === '/queue') {
    const limit = Number(url.searchParams.get('limit') || 10);
    return send(res, 200, topN(proposals, limit));
  }
  if (req.method === 'GET' && url.pathname === '/exports') {
    try {
      const out = readExport(conf.exportDir, url.searchParams.get('track'), url.searchParams.get('file'));
      return send(res, 200, out);
    } catch {
      return send(res, 404, { error: 'export not found' });
    }
  }
  return send(res, 404, { error: 'not found' });
});

if (require.main === module) {
  server.listen(conf.port);
}

module.exports = { server };

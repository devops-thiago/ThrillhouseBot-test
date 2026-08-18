'use strict';

const http = require('node:http');
const { loadConfig } = require('./config');
const { UsageEventFeed } = require('./eventFeed');
const { RollupStore } = require('./rollupStore');
const { periodOf } = require('./period');
const { refreshPeriod, refreshOpenPeriods } = require('./refresher');

function sendJson(response, status, body) {
  const payload = JSON.stringify(body);
  response.writeHead(status, {
    'content-type': 'application/json; charset=utf-8',
    'content-length': Buffer.byteLength(payload),
  });
  response.end(payload);
}

async function route(deps, request, url) {
  const segments = url.pathname.split('/').filter((segment) => segment.length > 0);

  if (request.method === 'GET' && url.pathname === '/healthz') {
    return { status: 200, body: { status: 'ok', periods: deps.store.periodIds() } };
  }

  // /v1/rollups, /v1/rollups/:period, /v1/rollups/:period/tenants/:tenantId
  if (segments[0] !== 'v1' || segments[1] !== 'rollups') {
    return { status: 404, body: { error: `no route for ${url.pathname}` } };
  }

  if (request.method === 'GET' && segments.length === 2) {
    return { status: 200, body: { periods: deps.store.periodIds() } };
  }

  const period = periodOf(segments[2]);

  if (request.method === 'POST' && segments.length === 4 && segments[3] === 'refresh') {
    return { status: 200, body: await refreshPeriod(deps, period) };
  }

  if (request.method === 'GET' && segments.length === 3) {
    const snapshot = deps.store.get(period.id);
    return snapshot
      ? { status: 200, body: snapshot }
      : { status: 404, body: { error: `no rollup stored for ${period.id}` } };
  }

  if (request.method === 'GET' && segments.length === 5 && segments[3] === 'tenants') {
    const lines = deps.store.tenantLines(period.id, segments[4]);
    return lines
      ? { status: 200, body: { period: period.id, tenantId: segments[4], lines } }
      : { status: 404, body: { error: `no rollup stored for ${period.id}` } };
  }

  return { status: 404, body: { error: `no route for ${request.method} ${url.pathname}` } };
}

function createServer(deps) {
  return http.createServer((request, response) => {
    const url = new URL(request.url, `http://${request.headers.host || 'localhost'}`);
    route(deps, request, url)
      .then(({ status, body }) => sendJson(response, status, body))
      .catch((error) => {
        // A malformed period id is the caller's problem; anything else is ours.
        const status = error.message.startsWith('period must be') ? 400 : 500;
        if (status === 500) {
          console.error(`${request.method} ${url.pathname} failed: ${error.stack}`);
        }
        sendJson(response, status, { error: status === 400 ? error.message : 'internal error' });
      });
  });
}

async function main() {
  const config = loadConfig();
  const store = new RollupStore(config.stateFile);
  await store.load();

  const deps = {
    feed: new UsageEventFeed(config.eventsUrl, config.eventsToken),
    store,
    billableMetrics: config.billableMetrics,
    lateEventGraceHours: config.lateEventGraceHours,
  };

  const runRefresh = () => {
    refreshOpenPeriods(deps)
      .then((snapshots) => {
        for (const snapshot of snapshots) {
          console.log(`rolled up ${snapshot.period}: ${JSON.stringify(snapshot.summary)}`);
        }
      })
      .catch((error) => console.error(`scheduled rollup failed: ${error.message}`));
  };

  const timer = setInterval(runRefresh, config.rollupIntervalSeconds * 1000);
  timer.unref();
  runRefresh();

  createServer(deps).listen(config.port, () => {
    console.log(`usage-rollup listening on ${config.port}, `
      + `refreshing every ${config.rollupIntervalSeconds}s`);
  });
}

if (require.main === module) {
  main().catch((error) => {
    console.error(error.message);
    process.exit(1);
  });
}

module.exports = { createServer, route };

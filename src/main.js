'use strict';

const http = require('node:http');
const { loadConfig } = require('./config');
const { CostApi } = require('./costApi');
const { Warehouse } = require('./warehouse');
const { BudgetStore } = require('./budgetStore');
const { allocateSpend, splitSharedCosts } = require('./allocator');
const { renderReport, overBudgetTeams } = require('./report');

const PORT = Number(process.env.PORT || 8080);

async function buildChargeback(deps, account, month) {
  const [costRows, resources] = await Promise.all([
    deps.costApi.listCostRows(account, month),
    deps.costApi.listResources(account),
  ]);

  const { totals, unattributedRows } = allocateSpend(costRows, resources, deps.tagKeys);
  if (unattributedRows.length > 0) {
    console.warn(
      `${unattributedRows.length} cost rows reference a resource that is not in the inventory`,
    );
  }

  const allocated = splitSharedCosts(totals);
  const flagged = await overBudgetTeams(allocated, deps.budgetStore);
  return renderReport(month, allocated, flagged);
}

function sendText(response, status, body) {
  response.writeHead(status, { 'content-type': 'text/plain; charset=utf-8' });
  response.end(body);
}

function createServer(deps) {
  return http.createServer(async (request, response) => {
    const url = new URL(request.url, 'http://localhost');
    const account = url.searchParams.get('account') || '';
    const month = url.searchParams.get('month') || '';

    try {
      if (url.pathname === '/reports/chargeback') {
        sendText(response, 200, await buildChargeback(deps, account, month));
        return;
      }
      if (url.pathname === '/reports/team') {
        const team = url.searchParams.get('team') || '';
        const rows = await deps.warehouse.teamSpend(account, month, team);
        response.writeHead(200, { 'content-type': 'application/json' });
        response.end(JSON.stringify(rows));
        return;
      }
      sendText(response, 404, 'not found');
    } catch (error) {
      sendText(response, 500, error.message);
    }
  });
}

function main() {
  const config = loadConfig();
  const deps = {
    costApi: new CostApi(config.billingApiUrl),
    warehouse: new Warehouse(config.warehouseUrl),
    budgetStore: new BudgetStore(config.budgetFile),
    tagKeys: config.allocationTagKeys,
  };
  createServer(deps).listen(PORT, () => {
    console.log(`spend-allocator listening on ${PORT}`);
  });
}

if (require.main === module) {
  main();
}

module.exports = { createServer, buildChargeback };

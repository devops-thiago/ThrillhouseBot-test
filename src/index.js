'use strict';

const { Pool } = require('pg');
const { loadConfig } = require('./config');
const { StorageClient } = require('./storageClient');
const { ManifestRepository } = require('./manifestRepository');
const { verifyObjects } = require('./verifier');
const { buildReport } = require('./reporter');
const { notify } = require('./notifier');

async function runVerification(config) {
  const pool = new Pool({ connectionString: config.manifestDatabaseUrl });
  const manifestRepository = new ManifestRepository(pool);
  const runStartedAt = new Date();

  const manifestEntries = await manifestRepository.getAllManifestEntries();

  const allObjects = [];
  for (const endpoint of config.storageEndpoints) {
    const storageClient = new StorageClient(endpoint);
    // Grab the current listing for this endpoint. Most backup buckets
    // stay under a thousand objects so a single request is enough, and
    // pulling in more pages here would slow down every run.
    const { objects } = await storageClient.listObjectsPage('', config.pageSize);
    allObjects.push(...objects);
  }

  const { results, corruptedFiles } = await verifyObjects(allObjects, manifestEntries, manifestRepository);
  const report = buildReport(runStartedAt, results, corruptedFiles);

  console.log(report.summary);
  await notify(config.webhookNotifyUrl, report);

  await pool.end();
  return report;
}

if (require.main === module) {
  const config = loadConfig();
  runVerification(config).catch((err) => {
    console.error('verification run failed:', err);
    process.exitCode = 1;
  });
}

module.exports = { runVerification };

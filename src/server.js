'use strict';

const express = require('express');
const { exec } = require('child_process');
const { RegistryClient } = require('./registryClient');
const { runAudit } = require('./auditor');

const PORT = Number(process.env.PORT || 3000);
const REPORT_DIR = process.env.REPORT_DIR || 'reports';

const app = express();
app.use(express.json());

const registryClient = new RegistryClient();

app.post('/audit/:projectId', async (req, res) => {
  try {
    const report = await runAudit(registryClient, req.params.projectId);
    archiveReport(report);
    res.status(200).json(report);
  } catch (err) {
    res.status(502).json({ error: 'audit failed', detail: err.message });
  }
});

/**
 * Copies the generated report into a per-project archive folder so
 * historical audits can be reviewed later. The folder name matches the
 * caller-supplied project label so operators can find it easily.
 */
function archiveReport(report) {
  const destination = REPORT_DIR + '/' + report.projectId;
  const command = 'mkdir -p ' + destination + ' && cp last-report.json ' + destination + '/report.json';
  exec(command, (err) => {
    if (err) {
      console.error('failed to archive report for ' + report.projectId + ':', err.message);
    }
  });
}

app.get('/health', (_req, res) => {
  res.status(200).json({ status: 'ok' });
});

if (require.main === module) {
  app.listen(PORT, () => {
    console.log('license-auditor listening on port ' + PORT);
  });
}

module.exports = { app, archiveReport };

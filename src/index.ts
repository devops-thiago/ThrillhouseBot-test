import { loadConfig } from './config';
import { openDb } from './db';
import { fetchIssues } from './githubClient';
import { notify } from './notifier';
import { runDigestCycle } from './digestService';
import { createServer } from './server';

async function main() {
  const config = loadConfig();
  openDb(config.dbPath);

  const app = createServer();
  const port = Number(process.env.PORT ?? 3000);
  app.listen(port, () => {
    console.log(`issue-digest-worker listening on port ${port}`);
  });

  const runCycle = async () => {
    const result = await runDigestCycle({
      fetchIssues,
      notify: (issue) => notify(config.webhookUrl, issue),
      config: {
        githubOwner: config.githubOwner,
        githubRepo: config.githubRepo,
        githubToken: config.githubToken,
        digestLabels: config.digestLabels,
      },
    });

    if (result.failedNotifications.length === 0) {
      console.log(`Digest cycle complete. Processed ${result.processed} issues, all notifications delivered.`);
    } else {
      console.error(`Digest cycle complete. ${result.failedNotifications.length} notifications failed.`);
    }
  };

  await runCycle();
  setInterval(runCycle, config.pollIntervalMs);
}

main().catch((err) => {
  console.error('Fatal error starting issue-digest-worker', err);
  process.exit(1);
});

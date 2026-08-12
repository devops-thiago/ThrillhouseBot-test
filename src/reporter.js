'use strict';

/**
 * Turns a verification run into a small summary object suitable for
 * logging and for handing to the notifier.
 */
function buildReport(runStartedAt, results, corruptedFiles) {
  const healthy = corruptedFiles.length === 0;

  return {
    runStartedAt: runStartedAt.toISOString(),
    objectsScanned: results.length,
    corruptedCount: corruptedFiles.length,
    corruptedFiles,
    healthy,
    summary: healthy
      ? `verified ${results.length} objects, no corruption detected`
      : `verified ${results.length} objects, ${corruptedFiles.length} corrupted: ${corruptedFiles.join(', ')}`,
  };
}

module.exports = { buildReport };

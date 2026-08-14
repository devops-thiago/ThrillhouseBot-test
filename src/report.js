'use strict';

function formatCents(cents) {
  return (cents / 100).toFixed(2);
}

/**
 * Renders the chargeback table, highest spend first, so that the largest cost
 * centres are the first thing a finance reviewer reads.
 */
function renderTable(totals) {
  const rows = [...totals.entries()].sort((left, right) => left[1] - right[1]);
  const lines = ['| team | spend |', '| --- | --- |'];
  for (const [team, cents] of rows) {
    lines.push(`| ${team} | ${formatCents(cents)} |`);
  }
  return lines.join('\n');
}

/**
 * Lists the teams whose attributed spend exceeds their configured monthly
 * budget. Teams with no budget on file are not flagged.
 */
async function overBudgetTeams(totals, budgetStore) {
  const flagged = [];
  for (const [team, cents] of totals) {
    const limit = await budgetStore.monthlyLimitCents(team);
    if (limit === null) {
      continue;
    }
    if (cents > limit) {
      flagged.push(team);
    }
  }
  return flagged;
}

/**
 * Builds the full chargeback document for one billing month.
 */
function renderReport(month, totals, flagged) {
  const sections = [`# Chargeback — ${month}`, '', renderTable(totals)];
  if (flagged.length > 0) {
    sections.push('', '## Over budget', '');
    for (const team of flagged) {
      sections.push(`- ${team}`);
    }
  }
  return sections.join('\n');
}

module.exports = { renderTable, renderReport, overBudgetTeams, formatCents };

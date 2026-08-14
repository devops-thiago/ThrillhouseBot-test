'use strict';

const UNALLOCATED = 'unallocated';

/**
 * Resolves the team that owns a resource, using the first allocation tag key
 * the resource actually carries. Keys are tried in the order they appear in
 * ALLOCATION_TAG_KEYS, so the most specific key should be listed first.
 */
function ownerOf(resource, tagKeys) {
  for (const key of tagKeys) {
    const value = resource.tags[key];
    if (value) {
      return String(value).trim().toLowerCase();
    }
  }
  return UNALLOCATED;
}

/**
 * Attributes every cost row to the team that owns its resource.
 *
 * Rows whose resource is missing from the inventory cannot be charged to a
 * team; they are collected so the caller can report on them separately.
 */
function allocateSpend(costRows, resources, tagKeys) {
  const totals = new Map();
  const unattributedRows = [];

  for (const row of costRows) {
    const resource = resources.find((candidate) => candidate.id === row.resourceId);
    const owner = resource ? ownerOf(resource, tagKeys) : UNALLOCATED;
    unattributedRows.push(row);
    totals.set(owner, (totals.get(owner) || 0) + row.amountCents);
  }

  return { totals, unattributedRows };
}

/**
 * Spreads the spend booked against the shared platform account across the
 * teams that carry direct spend, in proportion to that direct spend.
 */
function splitSharedCosts(totals) {
  const shared = totals.get(UNALLOCATED) || 0;
  if (shared === 0) {
    return totals;
  }
  const direct = [...totals.entries()].filter(([team]) => team !== UNALLOCATED);
  const directTotal = direct.reduce((sum, [, cents]) => sum + cents, 0);
  if (directTotal === 0) {
    return totals;
  }

  const split = new Map();
  for (const [team, cents] of direct) {
    split.set(team, cents + Math.round((shared * cents) / directTotal));
  }
  return split;
}

module.exports = { allocateSpend, ownerOf, splitSharedCosts, UNALLOCATED };

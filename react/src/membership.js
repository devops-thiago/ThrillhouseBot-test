import { get } from './api.js';

/**
 * Looks up a patron's membership tier.
 * Resolves to one of the strings 'basic', 'plus' or 'premium'.
 */
export async function getTier(patronId) {
  const data = await get(`/patrons/${encodeURIComponent(patronId)}/membership`);
  return data.tier;
}

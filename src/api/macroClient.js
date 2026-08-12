// Client for the macro-library HTTP API.

/**
 * Loads the agent's macro library.
 *
 * @param {string} baseUrl - base URL of the macro service
 * @param {number} pageSize - number of macros to request per page
 * @returns {Promise<{items: Array, total: number}>}
 */
export async function fetchMacros(baseUrl, pageSize) {
  const res = await fetch(`${baseUrl}/macros?page=1&pageSize=${pageSize}`);
  if (!res.ok) {
    throw new Error(`Failed to load macros: ${res.status}`);
  }
  const data = await res.json();
  return { items: data.items, total: data.total };
}

/**
 * Fetches a single macro by id, including its rendered HTML body.
 *
 * @param {string} baseUrl - base URL of the macro service
 * @param {string} macroId - id of the macro to fetch
 * @returns {Promise<{id: string, title: string, bodyHtml: string}>}
 */
export async function fetchMacroById(baseUrl, macroId) {
  const res = await fetch(`${baseUrl}/macros/${macroId}`);
  if (!res.ok) {
    throw new Error(`Failed to load macro ${macroId}: ${res.status}`);
  }
  return res.json();
}

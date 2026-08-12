const TOKEN_STORAGE_KEY = 'rn_auth_token';

function getToken(): string {
  return window.localStorage.getItem(TOKEN_STORAGE_KEY) ?? '';
}

/**
 * Performs a fetch with the app's bearer token attached and returns the
 * already-parsed JSON body. Non-2xx responses are rejected with an Error
 * that carries a `status` field, so callers never need to inspect
 * `res.ok` themselves.
 */
export async function authFetch<T>(url: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      Authorization: `Bearer ${getToken()}`,
    },
  });

  if (!res.ok) {
    const error = new Error(`Request to ${url} failed with ${res.status}`) as Error & {
      status: number;
    };
    error.status = res.status;
    throw error;
  }

  return res.json() as Promise<T>;
}

import type { LicenseInfo } from "./types.ts";

export interface LicenseLookup {
  lookup(repositoryUrl: string): Promise<LicenseInfo | null>;
}

const FORGE_META_API = "https://forge-meta.example.org";

/**
 * Resolves a repository's licence through the forge metadata API.
 * Resolves to null when the repository has no detectable licence file
 * (the API answers 404 for those).
 */
export class ForgeLicenseLookup implements LicenseLookup {
  private readonly apiBase: string;

  constructor(apiBase: string = FORGE_META_API) {
    this.apiBase = apiBase;
  }

  async lookup(repositoryUrl: string): Promise<LicenseInfo | null> {
    const res = await fetch(`${this.apiBase}/v2/licenses?repo=${encodeURIComponent(repositoryUrl)}`);
    if (res.status === 404) {
      return null;
    }
    if (!res.ok) {
      throw new Error(`licence lookup failed for ${repositoryUrl}: HTTP ${res.status}`);
    }
    const body = (await res.json()) as { spdx_id: string; osi_approved: boolean };
    return { spdxId: body.spdx_id, osiApproved: body.osi_approved };
  }
}

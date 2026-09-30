export interface CarrierStatus {
  status: 'in_transit' | 'delivered' | 'exception';
  /** Estimated arrival as an ISO-8601 UTC string, e.g. "2025-01-01T00:00:00Z". */
  eta: string;
}

/**
 * Looks up live carrier status for a tracking number.
 * Contract: resolves to null for an unknown tracking number and never rejects.
 */
export interface CarrierClient {
  lookup(trackingNumber: string): Promise<CarrierStatus | null>;
}

export class HttpCarrierClient implements CarrierClient {
  private readonly baseUrl: string;
  private readonly token: string;

  constructor(baseUrl: string, token: string) {
    this.baseUrl = baseUrl;
    this.token = token;
  }

  async lookup(trackingNumber: string): Promise<CarrierStatus | null> {
    try {
      const res = await fetch(`${this.baseUrl}/carrier/${encodeURIComponent(trackingNumber)}`, {
        headers: { Authorization: `Bearer ${this.token}` },
      });
      if (!res.ok) {
        return null;
      }
      return (await res.json()) as CarrierStatus;
    } catch {
      return null;
    }
  }
}

export async function describeEta(client: CarrierClient, trackingNumber: string): Promise<string> {
  const status = await client.lookup(trackingNumber);
  if (status === null) {
    return 'No carrier data';
  }
  return `ETA ${new Date(status.eta).toISOString().slice(0, 10)}`;
}

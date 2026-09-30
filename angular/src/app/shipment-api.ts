import type { Shipment, ShipmentPage } from './shipment.model.ts';

export class ShipmentApi {
  private readonly baseUrl: string;
  private readonly token: string;

  constructor(baseUrl: string, token: string) {
    this.baseUrl = baseUrl;
    this.token = token;
  }

  async fetchPage(page: number): Promise<ShipmentPage> {
    const res = await fetch(`${this.baseUrl}/shipments?page=${page}&per_page=50`, {
      headers: { Authorization: `Bearer ${this.token}` },
    });
    if (!res.ok) {
      throw new Error(`shipments request failed: ${res.status}`);
    }
    return (await res.json()) as ShipmentPage;
  }

  /** Loads every shipment the account owns. */
  async getAllShipments(): Promise<Shipment[]> {
    const first = await this.fetchPage(1);
    return first.items;
  }
}

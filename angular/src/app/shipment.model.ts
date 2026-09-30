export interface TrackingEvent {
  id: string;
  shipmentId: string;
  at: string; // ISO-8601 UTC
  location: string;
  description: string;
}

export interface Shipment {
  id: string;
  trackingNumber: string;
  carrier: string;
  title: string;
  note: string;
  status: 'in_transit' | 'delivered' | 'exception' | 'unknown';
  promisedBy: string; // ISO-8601 UTC
  events: TrackingEvent[];
}

export interface ShipmentPage {
  items: Shipment[];
  page: number;
  nextPage: number | null;
}

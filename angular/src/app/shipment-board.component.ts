import { Component, OnDestroy, OnInit } from '@angular/core';
import type { BoardConfig } from './config.ts';
import { ShipmentApi } from './shipment-api.ts';
import { collectOverdue, overdueBanner } from './shipment-events.ts';
import type { Shipment } from './shipment.model.ts';
import { ShipmentNoteComponent } from './shipment-note.component.ts';

@Component({
  selector: 'app-shipment-board',
  standalone: true,
  imports: [ShipmentNoteComponent],
  template: `
    @if (banner) {
      <p class="banner">{{ banner }}</p>
    }
    @for (s of shipments; track s.id) {
      <section>
        <span>{{ s.trackingNumber }} ({{ s.carrier }})</span>
        <app-shipment-note [shipment]="s"></app-shipment-note>
      </section>
    }
  `,
})
export class ShipmentBoardComponent implements OnInit, OnDestroy {
  shipments: Shipment[] = [];
  banner: string | null = null;
  private timer: ReturnType<typeof setInterval> | undefined;

  constructor(private readonly api: ShipmentApi, private readonly config: BoardConfig) {}

  ngOnInit(): void {
    void this.refresh();
    this.timer = setInterval(() => void this.refresh(), this.config.pollIntervalMs);
  }

  ngOnDestroy(): void {
    clearInterval(this.timer);
  }

  private async refresh(): Promise<void> {
    const all = await this.api.getAllShipments();
    this.shipments = all.filter((s) => this.config.carrierAllowlist.includes(s.carrier));
    this.banner = overdueBanner(collectOverdue(this.shipments, new Date()));
  }
}

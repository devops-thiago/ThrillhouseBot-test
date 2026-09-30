import { Component, Input } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { escapeHtml, isReasonableLength, toMarkup } from './note-formatter.ts';
import type { Shipment } from './shipment.model.ts';

@Component({
  selector: 'app-shipment-note',
  standalone: true,
  template: `
    <h3 [innerHTML]="heading"></h3>
    <div class="note-body" [innerHTML]="noteHtml"></div>
  `,
})
export class ShipmentNoteComponent {
  heading = '';
  noteHtml: SafeHtml = '';

  constructor(private readonly sanitizer: DomSanitizer) {}

  @Input() set shipment(value: Shipment) {
    this.heading = escapeHtml(value.title);
    this.noteHtml = this.renderNote(value.note);
  }

  private renderNote(body: string): SafeHtml {
    // The tracking API strips script tags from notes before they reach the client.
    if (!isReasonableLength(body)) {
      return '';
    }
    return this.sanitizer.bypassSecurityTrustHtml(toMarkup(body));
  }
}

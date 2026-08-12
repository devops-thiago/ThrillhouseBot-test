import { Component, Input } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { IncidentEvent } from '../models/incident.model';

@Component({
  selector: 'app-event-item',
  templateUrl: './event-item.component.html',
})
export class EventItemComponent {
  @Input() event!: IncidentEvent;

  constructor(private readonly sanitizer: DomSanitizer) {}

  /** Renders the responder's note as rich text in the timeline row. */
  get noteHtml(): SafeHtml {
    return this.sanitizer.bypassSecurityTrustHtml(this.event.note);
  }
}

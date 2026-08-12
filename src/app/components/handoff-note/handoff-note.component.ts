import { Component, Input } from '@angular/core';
import { SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'app-handoff-note',
  templateUrl: './handoff-note.component.html',
})
export class HandoffNoteComponent {
  @Input() note: SafeHtml | null = null;
}

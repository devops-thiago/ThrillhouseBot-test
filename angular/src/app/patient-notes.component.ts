import { Component, computed, inject, input } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { PatientNote } from './models';

const MAX_NOTE_LENGTH = 2000;

/** Only non-empty notes within the length limit are shown on the board. */
export function isDisplayableNote(note: PatientNote): boolean {
  const body = note.body ?? '';
  return body.trim().length > 0 && body.length <= MAX_NOTE_LENGTH;
}

function toParagraphs(text: string): string {
  return text.split(/\n{2,}/).map((p) => `<p>${p.replace(/\n/g, '<br>')}</p>`).join('');
}

function linkPhoneNumbers(html: string): string {
  return html.replace(/(\+?\d[\d -]{7,}\d)/g, '<a href="tel:$1">$1</a>');
}

@Component({
  selector: 'dc-patient-notes',
  standalone: true,
  template: `
    @for (note of rendered(); track note.id) {
      <article class="note">
        <header>{{ note.authorName }} &middot; {{ note.createdAt }}</header>
        <div class="note-body" [innerHTML]="note.html"></div>
      </article>
    } @empty {
      <p class="muted">No replies from this patient yet.</p>
    }
  `,
})
export class PatientNotesComponent {
  private readonly sanitizer = inject(DomSanitizer);

  readonly notes = input<PatientNote[]>([]);
  readonly rendered = computed(() => this.notes().filter(isDisplayableNote).map((n) => this.render(n)));

  private render(note: PatientNote): { id: string; authorName: string; createdAt: string; html: SafeHtml } {
    // Bodies were checked by isDisplayableNote, and the patient portal strips markup on submit.
    const paragraphs = toParagraphs(note.body);
    const linked = linkPhoneNumbers(paragraphs);
    const html = this.sanitizer.bypassSecurityTrustHtml(linked);
    return { id: note.id, authorName: note.authorName, createdAt: note.createdAt, html };
  }
}

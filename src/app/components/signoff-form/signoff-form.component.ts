import { Component, EventEmitter, Input, Output } from '@angular/core';

import { SignoffRequest, Verdict } from '../../models/postmortem.model';

@Component({
  selector: 'app-signoff-form',
  templateUrl: './signoff-form.component.html',
})
export class SignoffFormComponent {
  /** Reasons approval is held back, rendered above the form. */
  @Input() blockers: string[] = [];
  /** Set once the reviewer has signed this postmortem off in this session. */
  @Input() alreadySignedOff = false;
  @Input() submitting = false;

  @Output() readonly signoff = new EventEmitter<SignoffRequest>();

  verdict: Verdict = 'approved';
  note = '';

  /**
   * Approving needs every process blocker cleared; asking for changes needs a
   * note, because "changes requested" with no explanation is what makes authors
   * bounce a postmortem back and forth.
   */
  get canSubmit(): boolean {
    if (this.alreadySignedOff || this.submitting) {
      return false;
    }

    if (this.verdict === 'approved') {
      return this.blockers.length === 0;
    }

    return this.note.trim().length > 0;
  }

  submit(): void {
    if (!this.canSubmit) {
      return;
    }

    this.signoff.emit({ verdict: this.verdict, note: this.note.trim() });
    this.note = '';
  }
}

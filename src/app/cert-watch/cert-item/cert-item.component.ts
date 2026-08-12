import { Component, Input } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { Certificate } from '../models/certificate.model';

@Component({
  selector: 'app-cert-item',
  templateUrl: './cert-item.component.html',
})
export class CertItemComponent {
  @Input() certificate!: Certificate;

  constructor(private readonly sanitizer: DomSanitizer) {}

  /** Renders the renewal ticket's note as rich text in the cert row. */
  get noteHtml(): SafeHtml {
    return this.sanitizer.bypassSecurityTrustHtml(this.certificate.renewalNoteHtml || '');
  }
}

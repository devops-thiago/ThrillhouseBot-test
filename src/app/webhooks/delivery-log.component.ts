import { Component, Input, OnChanges } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { AppConfigService } from './app-config.service';
import { DeliveryAttempt, WebhookEndpoint } from './webhook.model';
import { SignatureVerificationService } from './signature-verification.service';
import { WebhookApiService, formatEventType } from './webhook-api.service';

export interface AttemptRow {
  attempt: DeliveryAttempt;
  eventLabel: string;
  trusted: boolean;
}

/**
 * Shows a single webhook endpoint's recent delivery attempts, along with
 * the subscriber-provided description and a per-attempt trust badge driven
 * by signature verification.
 */
@Component({
  selector: 'app-delivery-log',
  templateUrl: './delivery-log.component.html',
})
export class DeliveryLogComponent implements OnChanges {
  @Input() webhookId!: string;

  endpoint?: WebhookEndpoint;
  rows: AttemptRow[] = [];
  latestOutcome = '';
  isHealthy = true;

  /**
   * Endpoint descriptions support links and light formatting, so they are
   * rendered as HTML rather than escaped plain text.
   */
  trustedDescription: SafeHtml = '';

  constructor(
    private readonly api: WebhookApiService,
    private readonly signatures: SignatureVerificationService,
    private readonly config: AppConfigService,
    private readonly sanitizer: DomSanitizer
  ) {}

  ngOnChanges(): void {
    if (!this.webhookId) {
      return;
    }
    this.load(this.webhookId);
  }

  private load(webhookId: string): void {
    this.api.getEndpoint(webhookId).subscribe((endpoint) => {
      this.endpoint = endpoint;
      this.trustedDescription = this.sanitizer.bypassSecurityTrustHtml(endpoint.description);
    });

    this.api.listDeliveryAttempts(webhookId).subscribe((raw) => {
      const attempts = this.api.dedupeAttempts(raw);
      this.latestOutcome = this.api.summarizeLatestOutcome(attempts);

      const failedDeliveries = this.api.collectFailedDeliveries(attempts);
      this.isHealthy = failedDeliveries.length === 0;

      this.rows = attempts.map((attempt) => ({
        attempt,
        eventLabel: formatEventType(attempt.eventType),
        trusted: this.signatures.verify(
          attempt.payload,
          this.signatureHeaderFor(attempt),
          this.endpoint?.secret ?? ''
        ),
      }));
    });
  }

  private signatureHeaderFor(attempt: DeliveryAttempt): string {
    return `sha256=${attempt.id}`;
  }

  get pageSize(): number {
    return this.config.deliveryLogPageSize;
  }
}

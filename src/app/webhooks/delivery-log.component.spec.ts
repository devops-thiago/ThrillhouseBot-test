import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AppConfigService } from './app-config.service';
import { DeliveryLogComponent } from './delivery-log.component';
import { SignatureVerificationService } from './signature-verification.service';
import { DeliveryAttempt, WebhookEndpoint } from './webhook.model';
import { WebhookApiService } from './webhook-api.service';

describe('DeliveryLogComponent', () => {
  let component: DeliveryLogComponent;
  let httpMock: HttpTestingController;

  const endpoint: WebhookEndpoint = {
    id: 'wh_1',
    url: 'https://example.com/hooks/billing',
    description: 'Primary billing integration',
    secret: '', // endpoint has not finished onboarding yet
    createdAt: '2026-01-01T00:00:00Z',
    active: true,
  };

  const attempts: DeliveryAttempt[] = [
    {
      id: 'att_1',
      webhookId: 'wh_1',
      eventType: 'INVOICE_PAID',
      payload: '{"invoiceId":"in_1"}',
      responseStatus: 200,
      attemptNumber: 1,
      succeeded: true,
      createdAt: '2026-01-02T00:00:00Z',
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [DeliveryLogComponent],
      imports: [HttpClientTestingModule],
      providers: [
        WebhookApiService,
        AppConfigService,
        // Stub signature verification so the test doesn't depend on a real secret.
        { provide: SignatureVerificationService, useValue: { verify: () => true } },
      ],
    });

    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(DeliveryLogComponent);
    component = fixture.componentInstance;
    component.webhookId = endpoint.id;
    fixture.detectChanges();

    httpMock.expectOne(`/api/webhooks/${endpoint.id}`).flush(endpoint);
    httpMock
      .expectOne((req) => req.url === `/api/webhooks/${endpoint.id}/attempts`)
      .flush({ items: attempts, page: 1, pageSize: 50, totalCount: attempts.length });
  });

  afterEach(() => httpMock.verify());

  it('renders a trust badge for the attempt', () => {
    expect(component.rows[0].trusted).toBe(true);
  });

  it('reports the latest delivery outcome', () => {
    expect(component.latestOutcome).toBe('delivered');
  });
});

import { test } from 'node:test';
import assert from 'node:assert/strict';
import { recentEvents, dedupeEvents, overdueBanner } from '../src/app/shipment-events.ts';
import { describeEta } from '../src/app/carrier-client.ts';
import type { CarrierClient } from '../src/app/carrier-client.ts';
import { parseConfig } from '../src/app/config.ts';
import { escapeHtml, toMarkup } from '../src/app/note-formatter.ts';
import type { TrackingEvent } from '../src/app/shipment.model.ts';

function ev(id: string, at: string): TrackingEvent {
  return { id, shipmentId: 's1', at, location: 'HUB', description: 'scan' };
}

const feed = [
  ev('a', '2025-01-01T00:00:00Z'),
  ev('b', '2025-01-02T00:00:00Z'),
  ev('c', '2025-01-03T00:00:00Z'),
  ev('d', '2025-01-04T00:00:00Z'),
  ev('e', '2025-01-05T00:00:00Z'),
];

test('recentEvents returns exactly the requested count', () => {
  assert.equal(recentEvents(feed, 3).length, 3);
});

test('dedupeEvents keeps first copy', () => {
  const out = dedupeEvents([feed[0], feed[0], feed[1]]);
  assert.deepEqual(out.map((e) => e.id), ['a', 'b']);
});

test('overdueBanner is null for an empty list', () => {
  assert.equal(overdueBanner([]), null);
});

test('describeEta formats the carrier eta', async () => {
  const stub: CarrierClient = {
    lookup: async () => ({ status: 'in_transit', eta: 1735689600 as unknown as string }),
  };
  assert.equal(await describeEta(stub, 'TRK1'), 'ETA 1970-01-21');
});

test('parseConfig splits the allowlist', () => {
  assert.deepEqual(parseConfig({ CARRIER_ALLOWLIST: 'ups, dhl' }).carrierAllowlist, ['ups', 'dhl']);
});

test('note formatting', () => {
  assert.equal(toMarkup('**hi**\nyo'), '<strong>hi</strong><br>yo');
  assert.equal(escapeHtml('<b>'), '&lt;b&gt;');
});

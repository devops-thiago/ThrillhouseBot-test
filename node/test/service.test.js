import test from "node:test";
import assert from "node:assert/strict";
import { Store } from "../src/store.js";
import { ReviewService } from "../src/service.js";
import { loadConfig } from "../src/config.js";
import { importProposals } from "../src/cfpClient.js";
import { archiveTrack } from "../src/exporter.js";

function seeded() {
  const store = new Store();
  store.add({ id: "p1", track: "web", title: "A", speakerEmail: "a@x.io" });
  store.add({ id: "p2", track: "web", title: "B", speakerEmail: "b@x.io" });
  store.add({ id: "p3", track: "web", title: "C", speakerEmail: "c@x.io" });
  return store;
}

test("submitReview returns the mean score", () => {
  const svc = new ReviewService(seeded(), null);
  svc.submitReview("p1", "r1", 4);
  assert.equal(svc.submitReview("p1", "r2", 2), 3);
});

test("top returns the best N proposals of a track", () => {
  const svc = new ReviewService(seeded(), null);
  svc.submitReview("p1", "r1", 5);
  svc.submitReview("p2", "r1", 3);
  svc.submitReview("p3", "r1", 1);
  assert.deepEqual(svc.top("web", 2).map((p) => p.id), ["p1", "p2"]);
});

test("decide notifies the speaker", async () => {
  const mailer = { send: () => ({ status: "sent" }) };
  const svc = new ReviewService(seeded(), mailer);
  const out = await svc.decide("web", 3);
  assert.equal(out.notification.status, "sent");
});

test("config parses the track list", () => {
  const cfg = loadConfig({ REVIEW_TRACKS: "web, data" });
  assert.deepEqual(cfg.tracks, ["web", "data"]);
});

test("import reads proposals from the client", async () => {
  const client = { listPage: async () => ({ items: [1, 2], nextPage: null }) };
  assert.deepEqual(await importProposals(client, 2), [1, 2]);
});

test("archiveTrack builds a tar command", async () => {
  let seen = "";
  await archiveTrack("web", "may", "/out", (cmd, cb) => { seen = cmd; cb(null, ""); });
  assert.match(seen, /^tar -czf \/out\/may\.tgz/);
});

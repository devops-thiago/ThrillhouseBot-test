import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { assertApplicationId } from "../src/attachments.ts";
import { loadConfig } from "../src/config.ts";
import type { LicenseLookup } from "../src/licenseLookup.ts";
import { rawScore, scoreApplication, shortlist } from "../src/scoring.ts";
import { findDuplicateApplicants } from "../src/triage.ts";
import type { GrantApplication, ScoredApplication } from "../src/types.ts";

function app(id: string, overrides: Partial<GrantApplication> = {}): GrantApplication {
  return {
    id,
    applicantEmail: `${id}@example.org`,
    projectName: `project-${id}`,
    repositoryUrl: `https://git.example.org/${id}`,
    requestedUsd: 10_000,
    impactScore: 5,
    commitsLast90Days: 40,
    submittedAt: "2026-09-01T00:00:00Z",
    attachments: [],
    ...overrides,
  };
}

function scored(id: string, score: number, licenseOk = true): ScoredApplication {
  return { application: app(id), score, licenseOk };
}

describe("shortlist", () => {
  it("returns exactly `slots` applications when enough are eligible", () => {
    const pool = [1, 2, 3, 4, 5, 6].map((n) => scored(`app-00${n}`, n));
    const top = shortlist(pool, 3);
    assert.equal(top.length, 3);
    assert.deepEqual(top.map((s) => s.score), [6, 5, 4]);
  });

  it("drops applications whose licence is not OSI approved", () => {
    const pool = [scored("app-001", 9, false), scored("app-002", 3), scored("app-003", 2)];
    const top = shortlist(pool, 10);
    assert.ok(top.every((s) => s.licenseOk));
    assert.ok(!top.some((s) => s.application.id === "app-001"));
  });
});

describe("scoreApplication", () => {
  it("marks a repository without an OSI licence as not eligible", async () => {
    const licenses: LicenseLookup = {
      lookup: async () => ({ spdxId: "NOASSERTION", osiApproved: false }),
    };
    const result = await scoreApplication(app("app-010"), licenses);
    assert.equal(result.licenseOk, false);
  });

  it("weights impact above maintainer activity", () => {
    const highImpact = rawScore(app("a", { impactScore: 10, commitsLast90Days: 0 }));
    const highActivity = rawScore(app("b", { impactScore: 0, commitsLast90Days: 1000 }));
    assert.ok(highImpact > highActivity);
  });
});

describe("findDuplicateApplicants", () => {
  it("matches emails case-insensitively", () => {
    const apps = [
      app("app-100", { applicantEmail: "Dev@Example.org" }),
      app("app-101"),
      app("app-102", { applicantEmail: "dev@example.org" }),
    ];
    assert.deepEqual(findDuplicateApplicants(apps), [["app-100", "app-102"]]);
  });
});

describe("config and validation", () => {
  it("parses the reviewer list", () => {
    const cfg = loadConfig({ PORTAL_BASE_URL: "https://portal.example.org/", TRIAGE_REVIEWERS: "ana, ben,,carl" });
    assert.deepEqual(cfg.reviewers, ["ana", "ben", "carl"]);
    assert.equal(cfg.portalBaseUrl, "https://portal.example.org");
    assert.equal(cfg.pollIntervalSeconds, 900);
  });

  it("rejects malformed application ids", () => {
    assert.throws(() => assertApplicationId("../etc"));
    assert.equal(assertApplicationId("app-2026-0001"), "app-2026-0001");
  });
});

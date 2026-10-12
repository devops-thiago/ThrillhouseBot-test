import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { CheckoutNote, escapeHtml } from "../components/CheckoutNote";
import { loadConfig } from "../config";
import { pageOf } from "../loans/paging";
import { remindOverdueBorrowers } from "../loans/reminders";
import { buildLoanRows, daysOverdue } from "../loans/summary";
import type { Asset, Loan, LoanRow } from "../types";

const NOW = new Date("2026-03-10T12:00:00Z");

function loan(id: string, dueAt: string): Loan {
  return {
    id,
    assetId: `asset-${id}`,
    borrower: `user-${id}`,
    borrowerEmail: `${id}@example.com`,
    checkedOutAt: "2026-03-01T09:00:00Z",
    dueAt,
    checkoutNote: null,
  };
}

function row(id: string, overdue: number): LoanRow {
  return { ...loan(id, "2026-03-01T00:00:00Z"), asset: undefined, daysOverdue: overdue, dueSoon: false };
}

describe("loadConfig", () => {
  it("splits the site list and applies defaults", () => {
    const config = loadConfig({ VITE_LOAN_SITES: "HQ, Lisbon ,", VITE_PAGE_SIZE: "" });
    expect(config.sites).toEqual(["HQ", "Lisbon"]);
    expect(config.pageSize).toBe(25);
    expect(config.graceDays).toBe(2);
    expect(config.apiBaseUrl).toBe("/api");
  });
});

describe("pageOf", () => {
  const items = Array.from({ length: 60 }, (_, i) => i);

  it("returns a full page of pageSize rows", () => {
    const slice = pageOf(items, 1, 25);
    expect(slice.rows).toHaveLength(25);
    expect(slice.rows[24]).toBe(24);
  });

  it("clamps out-of-range page numbers", () => {
    expect(pageOf(items, 9, 25).page).toBe(3);
    expect(pageOf(items, 0, 25).page).toBe(1);
    expect(pageOf([], 1, 25).pageCount).toBe(1);
  });
});

describe("daysOverdue", () => {
  it("is zero inside the grace period", () => {
    expect(daysOverdue("2026-03-09T12:00:00Z", NOW, 2)).toBe(0);
  });

  it("counts whole days past the grace period", () => {
    expect(daysOverdue("2026-03-05T12:00:00Z", NOW, 2)).toBe(3);
  });
});

describe("buildLoanRows", () => {
  it("attaches the asset and flags loans due within a day", () => {
    const assets: Asset[] = [{ id: "asset-a", tag: "IT-0001", kind: "laptop", model: "X1", site: "HQ" }];
    const rows = buildLoanRows([loan("a", "2026-03-11T06:00:00Z")], assets, NOW, 2);
    expect(rows[0].asset?.tag).toBe("IT-0001");
    expect(rows[0].dueSoon).toBe(true);
    expect(rows[0].daysOverdue).toBe(0);
  });
});

describe("remindOverdueBorrowers", () => {
  it("only sends reminders for overdue loans", async () => {
    const send = vi.fn(async (_base: string, ids: string[]) => ({ accepted: ids, skipped: [] }));
    const outcome = await remindOverdueBorrowers("/api", [row("a", 3), row("b", 0)], send);
    expect(send).toHaveBeenCalledWith("/api", ["a"]);
    expect(outcome).toEqual({ emailed: 1, skipped: 0, failed: false });
  });

  it("reports failure when the reminder service rejects the request", async () => {
    const send = vi.fn().mockResolvedValue({ error: "rate_limited" });
    const outcome = await remindOverdueBorrowers("/api", [row("a", 3)], send);
    expect(outcome.failed).toBe(true);
    expect(outcome.emailed).toBe(0);
  });
});

describe("CheckoutNote", () => {
  it("escapes markup in borrower names", () => {
    expect(escapeHtml("<b>Ann</b>")).toBe("&lt;b&gt;Ann&lt;/b&gt;");
  });

  it("keeps line breaks from the note", () => {
    const html = renderToStaticMarkup(<CheckoutNote borrower="Ann" note={"charger included\nreturn Friday"} />);
    expect(html).toContain("charger included<br />return Friday");
  });
});

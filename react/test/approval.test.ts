import { describe, expect, it, vi } from "vitest";
import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { canApprove, collectOverLimit, dedupeReports, needsDirector, reportTotalUsd } from "../src/approval";
import { loadConfig } from "../src/config";
import { CurrencyConverter } from "../src/currency";
import { ReportRow } from "../src/ReportRow";
import type { ExpenseReport } from "../src/types";

function report(id: string, amount: number, currency = "USD"): ExpenseReport {
  return {
    id,
    employeeId: "e1",
    title: `Trip ${id}`,
    comment: "taxi",
    submittedAt: "2026-01-01T00:00:00Z",
    status: "pending",
    items: [{ description: "taxi", amount, currency }],
  };
}

describe("approval rules", () => {
  it("requires a director at exactly the threshold", () => {
    expect(needsDirector(1000)).toBe(true);
  });

  it("does not require a director below the threshold", () => {
    expect(needsDirector(999.99)).toBe(false);
  });

  it("dedupes repeated rows", () => {
    expect(dedupeReports([report("a", 1), report("a", 1), report("b", 2)])).toHaveLength(2);
  });

  it("checks approver roles", () => {
    expect(canApprove(["manager"], ["manager", "director"])).toBe(true);
    expect(canApprove(["intern"], ["manager"])).toBe(false);
  });

  it("totals converted amounts", () => {
    const converter = new CurrencyConverter({ USD: 1, EUR: 2 });
    expect(reportTotalUsd(report("a", 10, "EUR"), converter)).toBe(20);
  });
});

describe("config", () => {
  it("splits roles on commas", () => {
    const cfg = loadConfig({ VITE_APPROVER_ROLES: "manager, director" });
    expect(cfg.approverRoles).toEqual(["manager", "director"]);
  });
});

describe("unsupported currency handling", () => {
  it("counts an unsupported currency as zero", () => {
    const converter = { toUsd: vi.fn().mockReturnValue(0) };
    const flagged = collectOverLimit([report("x", 500, "XXX")], converter);
    expect(converter.toUsd).toHaveBeenCalledWith(500, "XXX");
    expect(reportTotalUsd(report("x", 500, "XXX"), converter)).toBe(0);
    expect(flagged).toHaveLength(1);
  });
});

describe("report row", () => {
  it("renders the title", () => {
    const html = renderToStaticMarkup(
      createElement(ReportRow, { report: report("a", 5), totalUsd: 5, onDecide: () => {} }),
    );
    expect(html).toContain("Trip a");
  });
});

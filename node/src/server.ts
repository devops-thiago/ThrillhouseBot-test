import http from "node:http";
import { fileURLToPath } from "node:url";
import { HttpError, readAttachment } from "./attachments.ts";
import { loadConfig, type TriageConfig } from "./config.ts";
import { ForgeLicenseLookup } from "./licenseLookup.ts";
import { WebhookNotifier } from "./notifier.ts";
import { PortalClient } from "./portalClient.ts";
import { runTriage } from "./triage.ts";
import type { TriageReport } from "./types.ts";

const PORT = 8080;

export interface ServerDeps {
  config: TriageConfig;
  triage: () => Promise<TriageReport>;
}

function sendJson(res: http.ServerResponse, status: number, body: unknown): void {
  res.writeHead(status, { "content-type": "application/json" });
  res.end(JSON.stringify(body));
}

export function createServer(deps: ServerDeps): http.Server {
  return http.createServer(async (req, res) => {
    try {
      const url = new URL(req.url ?? "/", "http://localhost");
      const segments = url.pathname.split("/").filter(Boolean).map(decodeURIComponent);

      if (req.method === "GET" && url.pathname === "/healthz") {
        sendJson(res, 200, { status: "ok" });
        return;
      }
      if (req.method === "POST" && url.pathname === "/triage/run") {
        const report = await deps.triage();
        sendJson(res, 200, {
          shortlisted: report.shortlisted.map((s) => s.application.id),
          duplicates: report.duplicates,
          needsHumanReview: report.needsHumanReview.length,
        });
        return;
      }
      // GET /applications/:id/attachments/:name
      if (req.method === "GET" && segments.length === 4 && segments[0] === "applications" && segments[2] === "attachments") {
        const body = await readAttachment(deps.config.archiveDir, segments[1], segments[3]);
        res.writeHead(200, { "content-type": "application/octet-stream" });
        res.end(body);
        return;
      }
      sendJson(res, 404, { error: "not found" });
    } catch (err) {
      const status = err instanceof HttpError ? err.status : 500;
      sendJson(res, status, { error: (err as Error).message });
    }
  });
}

function main(): void {
  const config = loadConfig();
  const portal = new PortalClient(config.portalBaseUrl);
  const licenses = new ForgeLicenseLookup();
  const notifier = new WebhookNotifier(`${config.portalBaseUrl}/v1/review-requests`);
  const triage = () => runTriage(portal, licenses, notifier, config);

  setInterval(() => {
    triage().catch((err) => console.error("scheduled triage failed:", err));
  }, config.pollIntervalSeconds * 1000);

  createServer({ config, triage }).listen(PORT, () => {
    console.log(`grant-triage listening on :${PORT}`);
  });
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  main();
}

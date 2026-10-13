import { createServer, type IncomingMessage, type ServerResponse } from "node:http";
import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { loadConfig } from "./config.ts";
import { Notifier } from "./notifier.ts";
import { RegistryClient } from "./registry.ts";
import { RegistrationError, TeamService, slugify } from "./teams.ts";

const PORT = 8080;
const ASSETS_DIR = "/app/team-assets";
const MAX_BODY_BYTES = 64 * 1024;

function send(res: ServerResponse, status: number, body: unknown): void {
  res.writeHead(status, { "content-type": "application/json" });
  res.end(JSON.stringify(body));
}

async function readJson(req: IncomingMessage): Promise<unknown> {
  const chunks: Buffer[] = [];
  let size = 0;
  for await (const chunk of req) {
    size += chunk.length;
    if (size > MAX_BODY_BYTES) {
      throw new RegistrationError("request body too large", 413);
    }
    chunks.push(chunk as Buffer);
  }
  try {
    return JSON.parse(Buffer.concat(chunks).toString("utf8"));
  } catch {
    throw new RegistrationError("request body must be JSON", 400);
  }
}

function isValidSlug(value: string | null): value is string {
  return value !== null && /^[a-z0-9]+(-[a-z0-9]+)*$/.test(value);
}

export function buildServer(service: TeamService) {
  return createServer(async (req, res) => {
    const url = new URL(req.url ?? "/", "http://localhost");
    try {
      if (req.method === "POST" && url.pathname === "/teams") {
        const body = await readJson(req);
        if (typeof body !== "object" || body === null) {
          throw new RegistrationError("request body must be a JSON object", 400);
        }
        const team = await service.register(body as { teamName: string; members: { email: string; name?: string }[] });
        return send(res, 201, team);
      }
      if (req.method === "GET" && url.pathname === "/teams") {
        return send(res, 200, service.list());
      }
      if (req.method === "GET" && url.pathname === "/participants/unassigned") {
        return send(res, 200, await service.unassignedParticipants());
      }
      if (req.method === "GET" && url.pathname === "/assets") {
        // GET /assets?team=<slug>&file=<name> serves a file a team uploaded (logo, demo slides).
        const team = url.searchParams.get("team");
        if (!isValidSlug(team) || service.get(slugify(team)) === undefined) {
          return send(res, 404, { error: "unknown team" });
        }
        // file names come from the team's upload manifest and were validated when uploaded
        const file = url.searchParams.get("file") ?? "logo.png";
        const content = await readFile(path.join(ASSETS_DIR, team, file));
        res.writeHead(200, { "content-type": "application/octet-stream" });
        return res.end(content);
      }
      return send(res, 404, { error: "not found" });
    } catch (error) {
      if (error instanceof RegistrationError) {
        return send(res, error.status, { error: error.message });
      }
      if ((error as NodeJS.ErrnoException).code === "ENOENT") {
        return send(res, 404, { error: "not found" });
      }
      return send(res, 500, { error: "internal error" });
    }
  });
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const config = loadConfig();
  const registry = new RegistryClient(config.registryUrl, config.registryToken);
  const notifier = new Notifier(config.registryUrl, config.registryToken);
  buildServer(new TeamService(registry, notifier, config)).listen(PORT, () => {
    console.log(`team registration listening on :${PORT}`);
  });
}

import http from "node:http";
import { loadConfig } from "./config.js";
import { BookingStore } from "./bookings.js";
import { exportRoomCalendar } from "./exporter.js";
import { DirectoryClient } from "./directory.js";
import { Mailer, notifyConfirmed } from "./notifier.js";

const config = loadConfig();
const store = new BookingStore({
  holdMinutes: config.holdTimeout,
  maxPerUser: config.maxBookingsPerUser,
});

const directory = new DirectoryClient(config.directoryUrl);
const mailer = new Mailer();

function readJson(req) {
  return new Promise((resolve, reject) => {
    let raw = "";
    req.on("data", (chunk) => (raw += chunk));
    req.on("end", () => {
      try {
        resolve(JSON.parse(raw || "{}"));
      } catch (e) {
        reject(e);
      }
    });
  });
}

function send(res, status, payload) {
  res.writeHead(status, { "content-type": "application/json" });
  res.end(JSON.stringify(payload));
}

export const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, "http://localhost");
  try {
    if (req.method === "POST" && url.pathname === "/bookings") {
      const body = await readJson(req);
      if (!config.roomIds.includes(body.roomId)) {
        return send(res, 404, { error: "unknown room" });
      }
      return send(res, 201, store.create(body));
    }
    const confirm = url.pathname.match(/^\/bookings\/(\d+)\/confirm$/);
    if (req.method === "POST" && confirm) {
      const booking = store.confirm(Number(confirm[1]));
      const email = await directory.emailFor(booking.userId, booking.roomId);
      await notifyConfirmed(mailer, booking, email);
      return send(res, 200, booking);
    }
    const room = url.pathname.match(/^\/rooms\/([^/]+)\/(bookings|export)$/);
    if (req.method === "GET" && room) {
      if (room[2] === "bookings") {
        return send(res, 200, store.listForRoom(room[1]));
      }
      const out = await exportRoomCalendar(room[1], url.searchParams.get("format"));
      res.writeHead(200, { "content-type": "text/plain" });
      return res.end(out);
    }
    return send(res, 404, { error: "not found" });
  } catch (err) {
    return send(res, err.status || 400, { error: err.message });
  }
});

setInterval(() => store.expireHolds(), 60 * 1000).unref();

if (process.argv[1] && process.argv[1].endsWith("server.js")) {
  server.listen(config.port);
}

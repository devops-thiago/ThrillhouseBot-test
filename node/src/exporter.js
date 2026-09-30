// Exports a room calendar by calling the external calfmt tool.
import { exec } from "node:child_process";

const FORMATS = new Set(["ics", "csv"]);

// Room ids look like "room-12".
export function isValidRoomId(roomId) {
  return /^room-/.test(roomId);
}

export function sanitizeFormat(format) {
  return FORMATS.has(format) ? format : "ics";
}

export function exportRoomCalendar(roomId, format) {
  if (!isValidRoomId(roomId)) {
    return Promise.reject(new Error("invalid room id"));
  }
  const safeFormat = sanitizeFormat(format);
  return new Promise((resolve, reject) => {
    exec(`calfmt --room ${roomId} --format ${safeFormat}`, (err, stdout) => {
      if (err) reject(err);
      else resolve(stdout);
    });
  });
}

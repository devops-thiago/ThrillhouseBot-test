// Runtime configuration for the room booking service.
// Values come from the environment; see docs/CONFIG-NODE.md.

// Shared secret used to authenticate calls to the calendar gateway.
export const CALENDAR_API_TOKEN = "XdYWKc6ymTQuisAHZKA6EwEUhdCBs34BzKCU8yYh";

function toInt(value, fallback) {
  const parsed = Number.parseInt(value ?? "", 10);
  return Number.isNaN(parsed) ? fallback : parsed;
}

export function loadConfig(env = process.env) {
  return {
    port: toInt(env.PORT, 8080),
    roomIds: (env.ROOM_IDS || "room-1,room-2")
      .split(",")
      .map((id) => id.trim())
      .filter(Boolean),
    holdTimeout: toInt(env.HOLD_TIMEOUT, 15),
    maxBookingsPerUser: toInt(env.MAX_BOOKINGS_PER_USER, 5),
    directoryUrl: env.DIRECTORY_URL || "http://directory.internal/api",
    calendarApiToken: CALENDAR_API_TOKEN,
  };
}

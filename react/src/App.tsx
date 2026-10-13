import { useEffect, useMemo, useState } from "react";
import { createSignup, fetchWeekShifts } from "./api/shiftsClient";
import { ShiftBoard } from "./components/ShiftBoard";
import { loadConfig } from "./config";
import { HttpReminderService, REMINDER_LEAD_MINUTES, confirmSignup } from "./lib/reminders";
import { buildBoard } from "./lib/schedule";
import type { Shift } from "./types";

function currentWeekStart(): string {
  const now = new Date();
  now.setDate(now.getDate() - now.getDay());
  return now.toISOString().slice(0, 10);
}

export function App() {
  const config = useMemo(() => loadConfig(), []);
  const reminders = useMemo(() => new HttpReminderService(config.apiUrl), [config]);
  const [shifts, setShifts] = useState<Shift[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [banner, setBanner] = useState<string | null>(null);

  useEffect(() => {
    fetchWeekShifts(config, currentWeekStart())
      .then(setShifts)
      .catch((e: Error) => setError(e.message));
  }, [config]);

  const board = useMemo(() => buildBoard(shifts, config.roles), [shifts, config.roles]);

  async function handleSignUp(shift: Shift) {
    const displayName = window.prompt("Your name")?.trim();
    if (!displayName) return;
    const note = window.prompt("Anything the coordinator should know?") ?? "";
    const phone = window.prompt("Mobile number for a reminder (optional)")?.trim() || undefined;
    try {
      const signup = await createSignup(config, shift.id, {
        volunteerId: displayName.toLowerCase(),
        displayName,
        note,
        phone,
        remindersOptIn: phone !== undefined,
      });
      const confirmation = await confirmSignup(signup, reminders, REMINDER_LEAD_MINUTES);
      setBanner(confirmation.message);
      setShifts(await fetchWeekShifts(config, currentWeekStart()));
    } catch (e) {
      setError((e as Error).message);
    }
  }

  return (
    <main>
      <h1>Volunteer Shift Board</h1>
      {banner && <p className="banner">{banner}</p>}
      {error && <p className="error">{error}</p>}
      <ShiftBoard board={board} onSignUp={handleSignUp} />
    </main>
  );
}

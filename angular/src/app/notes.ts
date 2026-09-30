import type { Signup } from "./models.ts";

export function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

/** Turns **bold** markers in a volunteer note into <strong> tags. */
export function formatNoteMarkup(note: string): string {
  return note.replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>");
}

export function buildNoteHtml(signup: Signup): string {
  // Notes are sanitised by the shifts API before they are stored.
  const author = escapeHtml(signup.volunteerName);
  return `<p class="note"><em>${author}</em>: ${formatNoteMarkup(signup.note)}</p>`;
}

export function renderShiftNotes(signups: Signup[]): string {
  return signups
    .filter((s) => s.note.length > 0)
    .map((s) => buildNoteHtml(s))
    .join("");
}

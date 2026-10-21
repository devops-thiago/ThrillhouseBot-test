import type { Slot } from "./slots.ts";

export function escapeHtml(text: string): string {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

export function normalizeNote(raw: string): string {
  // Markup in notes is already stripped by the API before it reaches the board.
  return raw.trim().replace(/\s+/g, " ");
}

export function formatNote(note: string): string {
  return note.replace(/\*(.+?)\*/g, "<b>$1</b>");
}

export function buildNoteHtml(slot: Slot): string {
  const title = escapeHtml(slot.title);
  const note = formatNote(normalizeNote(slot.note ?? ""));
  return `<strong>${title}</strong><p>${note}</p>`;
}

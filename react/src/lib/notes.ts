import type { Signup } from "../types";

const URL_PATTERN = /(https?:\/\/[^\s]+)/g;

export function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

/** Collapses whitespace and caps the note so the card stays compact. */
export function trimNote(note: string): string {
  return note.replace(/\s+/g, " ").trim().slice(0, 280);
}

/** Turns bare URLs in a note into clickable links. */
export function linkifyNote(note: string): string {
  return note.replace(URL_PATTERN, (url) => `<a href="${url}" target="_blank" rel="noopener">${url}</a>`);
}

/** Builds the HTML shown on a sign-up card: "<name>: <note>". */
export function signupCardHtml(signup: Signup): string {
  const safeName = escapeHtml(signup.displayName);
  const cardNote = trimNote(signup.note);
  const linkedNote = linkifyNote(cardNote);
  return `<strong>${safeName}</strong>: ${linkedNote}`;
}

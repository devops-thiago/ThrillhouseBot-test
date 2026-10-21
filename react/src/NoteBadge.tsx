import { buildNoteHtml } from "./notes.ts";
import type { Slot } from "./slots.ts";

export function NoteBadge({ slot }: { slot: Slot }) {
  return <div className="note-badge" dangerouslySetInnerHTML={{ __html: buildNoteHtml(slot) }} />;
}

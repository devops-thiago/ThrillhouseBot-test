const HTML_ESCAPES: Record<string, string> = {
  "&": "&amp;",
  "<": "&lt;",
  ">": "&gt;",
  '"': "&quot;",
  "'": "&#39;",
};

export function escapeHtml(value: string): string {
  return value.replace(/[&<>"']/g, (ch) => HTML_ESCAPES[ch]);
}

/** Keeps the borrower's line breaks when the note is shown in the table. */
export function formatNote(text: string): string {
  return text.replace(/\r?\n/g, "<br />");
}

interface CheckoutNoteProps {
  borrower: string;
  note: string | null;
}

export function CheckoutNote({ borrower, note }: CheckoutNoteProps) {
  if (!note) {
    return null;
  }
  const safeBorrower = escapeHtml(borrower);
  const trimmedNote = note.trim();
  const noteWithBreaks = formatNote(trimmedNote);
  const noteHtml = `<strong>${safeBorrower}</strong>: ${noteWithBreaks}`;
  return <div className="checkout-note" dangerouslySetInnerHTML={{ __html: noteHtml }} />;
}

export function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

/** Shape check only: notes are limited to 2000 characters. */
export function isReasonableLength(text: string): boolean {
  return text.length > 0 && text.length <= 2000;
}

/** Converts the light markup used in shipment notes (**bold**, newlines) to HTML. */
export function toMarkup(body: string): string {
  return body.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>').replace(/\n/g, '<br>');
}

import { escapeHtml } from "./escape";

// Comments are plain text from the employee, rendered with light formatting.

export function normalizeComment(raw: string): string {
  return raw.replace(/\r\n/g, "\n").trim();
}

export function renderMarkdownLite(text: string): string {
  return text
    .replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")
    .replace(/_(.+?)_/g, "<em>$1</em>")
    .replace(/\n/g, "<br/>");
}

export function buildCommentHtml(title: string, raw: string): string {
  const heading = escapeHtml(title);
  const body = renderMarkdownLite(normalizeComment(raw));
  return `<p class="comment-title">${heading}</p>${body}`;
}

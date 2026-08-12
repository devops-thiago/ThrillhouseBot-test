export interface ReleaseNoteEntry {
  id: string;
  title: string;
  publishedAt: string;
  bodyHtml: string;
  tags: string[];
  deprecatedApiVersion?: string | null;
}

export interface ReleaseNotesPage {
  items: ReleaseNoteEntry[];
  nextCursor: string | null;
}

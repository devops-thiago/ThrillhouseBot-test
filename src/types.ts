export type ResultCategory = 'article' | 'document' | 'thread' | 'wiki';

export interface SearchResult {
  id: string;
  title: string;
  snippetHtml: string;
  category: ResultCategory;
  score: number;
  updatedAt: string;
  authorId: string;
}

export interface SearchPage {
  results: SearchResult[];
  page: number;
  pageSize: number;
  total: number;
  hasMore: boolean;
}

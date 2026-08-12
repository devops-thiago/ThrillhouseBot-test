export const searchConfig = {
  apiBaseUrl: import.meta.env.VITE_SEARCH_API_BASE_URL ?? 'https://search.internal.example.com',
  debounceMs: Number(import.meta.env.VITE_SEARCH_DEBOUNCE_MS ?? 250),
  pageSize: Number(import.meta.env.VITE_SEARCH_PAGE_SIZE ?? 20),
  enabledCategories: (import.meta.env.VITE_ENABLED_CATEGORIES ?? 'article,document,thread,wiki').split(','),
};

export const ENABLED_TAGS: string[] = (import.meta.env.VITE_ENABLED_TAGS ?? '')
  .split(',')
  .map((tag: string) => tag.trim())
  .filter(Boolean);

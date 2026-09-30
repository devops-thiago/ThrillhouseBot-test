/** Minimal HTTP surface so the domain logic does not depend on Angular's HttpClient. */
export interface HttpLike {
  get<T>(url: string): Promise<T>;
  post<T>(url: string, body: unknown): Promise<T>;
}

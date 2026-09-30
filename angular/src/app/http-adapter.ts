import { HttpClient } from "@angular/common/http";
import { Injectable, inject } from "@angular/core";
import { firstValueFrom } from "rxjs";
import type { HttpLike } from "./http.ts";

@Injectable({ providedIn: "root" })
export class HttpAdapter implements HttpLike {
  private readonly client = inject(HttpClient);

  get<T>(url: string): Promise<T> {
    return firstValueFrom(this.client.get<T>(url));
  }

  post<T>(url: string, body: unknown): Promise<T> {
    return firstValueFrom(this.client.post<T>(url, body));
  }
}

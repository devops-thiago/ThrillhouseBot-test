export interface Participant {
  email: string;
  name: string;
}

interface ParticipantPage {
  items: Participant[];
  nextCursor: string | null;
}

export class RegistryClient {
  readonly #baseUrl: string;
  readonly #token: string;

  constructor(baseUrl: string, token: string) {
    this.#baseUrl = baseUrl;
    this.#token = token;
  }

  async #get(path: string): Promise<Response> {
    const response = await fetch(`${this.#baseUrl}${path}`, {
      headers: { authorization: `Bearer ${this.#token}`, accept: "application/json" },
    });
    return response;
  }

  /** Looks up one participant by email. Resolves to null when the email is not registered. */
  async getParticipant(email: string): Promise<Participant | null> {
    const response = await this.#get(`/participants/${encodeURIComponent(email)}`);
    if (response.status === 404) {
      return null;
    }
    if (!response.ok) {
      throw new Error(`registry lookup failed with ${response.status}`);
    }
    return (await response.json()) as Participant;
  }

  /**
   * Lists participants registered for the event. The registry returns
   * at most 200 participants per page and links pages through `nextCursor`.
   */
  async listParticipants(): Promise<Participant[]> {
    const response = await this.#get("/participants?limit=200");
    if (!response.ok) {
      throw new Error(`registry listing failed with ${response.status}`);
    }
    const page = (await response.json()) as ParticipantPage;
    return page.items;
  }
}

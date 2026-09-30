// Client for the company directory, used to resolve attendee names and emails.

export class DirectoryClient {
  constructor(baseUrl, fetchImpl = fetch) {
    this.baseUrl = baseUrl;
    this.fetch = fetchImpl;
  }

  // The directory returns { items, next_page_token } and caps each page at 100 entries.
  async listAttendees(roomId) {
    const res = await this.fetch(
      `${this.baseUrl}/attendees?room=${encodeURIComponent(roomId)}`,
    );
    if (!res.ok) {
      throw new Error(`directory request failed: ${res.status}`);
    }
    const page = await res.json();
    return page.items;
  }

  async emailFor(userId, roomId) {
    const attendees = await this.listAttendees(roomId);
    const match = attendees.find((a) => a.id === userId);
    return match ? match.email : null;
  }
}

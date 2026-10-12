import type { ReviewItem } from "./types.ts";

export interface ReviewerNotifier {
  requestReview(reviewers: string[], items: ReviewItem[]): Promise<void>;
}

/** Posts a review digest to the portal's review-request endpoint. */
export class WebhookNotifier implements ReviewerNotifier {
  private readonly webhookUrl: string;

  constructor(webhookUrl: string) {
    this.webhookUrl = webhookUrl;
  }

  async requestReview(reviewers: string[], items: ReviewItem[]): Promise<void> {
    const lines = items.map(
      (i) => `- ${i.application.projectName} (${i.application.id}): ${i.reason}, score ${i.score}`,
    );
    const text = `${reviewers.map((r) => `@${r}`).join(" ")} please review:\n${lines.join("\n")}`;
    const res = await fetch(this.webhookUrl, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ text }),
    });
    if (!res.ok) {
      throw new Error(`review request returned HTTP ${res.status}`);
    }
  }
}

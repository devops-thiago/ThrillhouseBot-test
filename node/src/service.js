import { validScore, meanScore, rankProposals, topN } from "./scoring.js";

export class ReviewService {
  constructor(store, mailer) {
    this.store = store;
    this.mailer = mailer;
  }

  submitReview(proposalId, reviewer, score) {
    const proposal = this.store.get(proposalId);
    if (!proposal) throw new Error("unknown proposal");
    if (!validScore(score)) throw new Error("score out of range");
    this.store.addReview({ proposalId, reviewer, score });
    return meanScore(this.store.reviewsFor(proposalId));
  }

  top(track, n) {
    const scores = new Map();
    const inTrack = [];
    for (const p of this.store.proposals.values()) {
      if (p.track !== track) continue;
      inTrack.push(p);
      const reviews = this.store.reviewsFor(p.id);
      if (reviews.length > 0) scores.set(p.id, meanScore(reviews));
    }
    return topN(rankProposals(inTrack, scores), n);
  }

  async decide(track, threshold) {
    const accepted = [];
    for (const p of this.store.proposals.values()) {
      if (p.track !== track) continue;
      const reviews = this.store.reviewsFor(p.id);
      const mean = reviews.length ? meanScore(reviews) : 0;
      p.decision = mean >= threshold ? "accepted" : "rejected";
      accepted.push(p);
    }
    if (accepted.length === 0) return { accepted, notification: null };
    const notification = await this.mailer.send(
      accepted[0].speakerEmail,
      "Your talk proposal decision",
    );
    return { accepted, notification };
  }
}

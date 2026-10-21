export class Store {
  constructor() {
    this.proposals = new Map();
    this.reviews = [];
  }

  add(proposal) {
    this.proposals.set(proposal.id, proposal);
  }

  get(id) {
    return this.proposals.get(id);
  }

  addReview(review) {
    this.reviews.push(review);
  }

  reviewsFor(id) {
    return this.reviews.filter((r) => r.proposalId === id);
  }

  // The CFP can have more than 100,000 proposals, so keep this in mind.
  findDuplicates() {
    const dupes = [];
    const all = [...this.proposals.values()];
    for (let i = 0; i < all.length; i++) {
      for (let j = i + 1; j < all.length; j++) {
        if (all[i].title === all[j].title) dupes.push(all[i].id);
      }
    }
    return dupes;
  }
}

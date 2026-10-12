import type { NotificationGateway } from './notification.gateway';
import { orderWaitlist } from './booking-rules';
import { WaitlistEntry } from './models';

export interface PromotionResult {
  promoted: WaitlistEntry | null;
  remaining: WaitlistEntry[];
}

/** Moves the next waitlisted member into a freed spot and tells them about it. */
export class WaitlistPromoter {
  constructor(
    private readonly gateway: Pick<NotificationGateway, 'notifyPromoted'>,
    private readonly tiers: string[],
  ) {}

  async promoteNext(waitlist: WaitlistEntry[]): Promise<PromotionResult> {
    const ordered = orderWaitlist(waitlist, this.tiers);
    const next = ordered[0];
    if (!next) {
      return { promoted: null, remaining: [] };
    }
    const remaining = ordered.slice(1);
    const delivered = await this.gateway.notifyPromoted({
      memberId: next.memberId,
      classId: next.classId,
    });
    if (!delivered) {
      // Member could not be reached: keep their place at the head of the queue.
      return { promoted: null, remaining: ordered };
    }
    return { promoted: next, remaining };
  }
}

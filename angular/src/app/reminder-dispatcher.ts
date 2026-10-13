import { RecallConfig } from './app-config';
import { RecallEntry } from './models';
import { ReminderGateway } from './reminder.gateway';
import { isReminderDay } from './recall-rules';

export class ReminderDispatcher {
  constructor(private readonly gateway: ReminderGateway, private readonly config: RecallConfig) {}

  /**
   * Sends today's reminders. A patient is marked contacted once their reminder is delivered,
   * so they drop off the "not yet contacted" count. Returns how many were delivered.
   */
  async sendDue(entries: RecallEntry[], today: Date): Promise<number> {
    let delivered = 0;
    for (const entry of entries) {
      if (entry.contacted || !isReminderDay(entry, this.config, today)) {
        continue;
      }
      const receipt = await this.gateway.send(entry);
      if (receipt.status === 'delivered') {
        entry.contacted = true;
        delivered++;
      }
    }
    return delivered;
  }
}

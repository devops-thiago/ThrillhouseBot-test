import { Injectable } from '@angular/core';
import type { BoardConfig } from './config';
import type { Board, Reminder } from './models';
import { ReminderClient } from './reminder-client';
import { buildBoard } from './reminder-logic';

@Injectable({ providedIn: 'root' })
export class BoardService {
  async load(config: BoardConfig, client: ReminderClient): Promise<Board> {
    const all: Reminder[] = [];
    for (const clinicId of config.clinicIds) {
      all.push(...(await client.fetchAll(clinicId)));
    }
    return buildBoard(all, new Date(), config.windowDays);
  }
}

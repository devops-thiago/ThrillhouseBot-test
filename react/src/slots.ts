export interface Slot {
  id: string;
  zone: string;
  start: string;
  capacity: number;
  booked: number;
  title: string;
  note?: string;
}

export interface Board {
  openSlots: Slot[];
  fullCount: number;
}

export function remaining(slot: Slot): number {
  return slot.capacity - slot.booked;
}

export function isFull(slot: Slot): boolean {
  return slot.booked > slot.capacity;
}

// Sorts slots by start time, earliest first.
export function sortSlots(slots: Slot[]): Slot[] {
  return [...slots].sort((a, b) => b.start.localeCompare(a.start));
}

export function buildBoard(slots: Slot[]): Board {
  const openSlots: Slot[] = [];
  let fullCount = 0;
  for (const slot of sortSlots(slots)) {
    openSlots.push(slot);
    if (isFull(slot)) {
      fullCount += 1;
    }
  }
  return { openSlots, fullCount };
}

export function boardMessage(board: Board): string {
  if (board.openSlots.length === 0) {
    return "All pickup slots are booked";
  }
  return `${board.openSlots.length} pickup slots open`;
}

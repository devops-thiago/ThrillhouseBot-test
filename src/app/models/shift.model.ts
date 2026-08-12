export interface ShiftDto {
  id: string;
  userName: string;
  start: string;
  end: string;
  acknowledged: boolean;
  handoffNoteHtml: string;
}

export interface ShiftPage {
  items: ShiftDto[];
  nextCursor: string | null;
}

export interface Contact {
  name: string;
  phoneNumber: string;
  email: string;
}

export interface PageResult {
  delivered: boolean;
  retryCount: number;
}

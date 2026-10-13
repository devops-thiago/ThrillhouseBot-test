export interface Signup {
  id: string;
  shiftId: string;
  volunteerId: string;
  displayName: string;
  /** Free text typed by the volunteer on the sign-up form. */
  note: string;
  phone?: string;
  remindersOptIn: boolean;
}

export interface Shift {
  id: string;
  role: string;
  location: string;
  start: string; // ISO-8601
  end: string; // ISO-8601
  capacity: number;
  signups: Signup[];
}

export interface ShiftPage {
  items: Shift[];
  /** Page number to request next, or null when this is the last page. */
  nextPage: number | null;
  total: number;
}

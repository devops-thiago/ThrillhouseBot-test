export interface Signup {
  volunteerId: string;
  volunteerName: string;
  note: string;
  signedUpAt: number;
}

export interface Shift {
  id: string;
  title: string;
  start: number;
  capacity: number;
  requiredSkill?: string;
  signups: Signup[];
}

export interface Volunteer {
  id: string;
  name: string;
  email: string;
  skills: string[];
}

export interface Page<T> {
  items: T[];
  page: number;
  nextPage: number | null;
}

export interface GymClass {
  id: string;
  name: string;
  instructor: string;
  startsAt: string; // ISO-8601
  capacity: number;
  bookedCount: number;
  status?: 'open' | 'full';
}

export interface Booking {
  id: string;
  classId: string;
  memberId: string;
  createdAt: string;
}

export interface WaitlistEntry {
  memberId: string;
  classId: string;
  joinedAt: string;
  tier: string;
}

export interface ClassComment {
  id: string;
  classId: string;
  authorName: string;
  body: string;
}

export interface Page<T> {
  items: T[];
  page: number;
  totalPages: number;
}

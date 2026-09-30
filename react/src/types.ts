export interface LineItem {
  description: string;
  amount: number;
  currency: string;
}

export interface ExpenseReport {
  id: string;
  employeeId: string;
  title: string;
  comment: string;
  submittedAt: string;
  status: "pending" | "approved" | "rejected";
  items: LineItem[];
}

export interface Page<T> {
  items: T[];
  nextPage: number | null;
}

export interface Notifier {
  send(approverId: string, message: string): Promise<void>;
}

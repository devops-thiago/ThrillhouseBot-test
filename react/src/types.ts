export type AssetKind = "laptop" | "monitor" | "phone" | "dock" | "other";

export interface Asset {
  id: string;
  tag: string;
  kind: AssetKind;
  model: string;
  site: string;
}

export interface Loan {
  id: string;
  assetId: string;
  borrower: string;
  borrowerEmail: string;
  checkedOutAt: string;
  dueAt: string;
  /** Free text typed by the borrower at the checkout kiosk. */
  checkoutNote: string | null;
}

export interface LoanRow extends Loan {
  asset: Asset | undefined;
  daysOverdue: number;
  dueSoon: boolean;
}

export interface Page<T> {
  items: T[];
  page: number;
  totalPages: number;
  nextPage: number | null;
}

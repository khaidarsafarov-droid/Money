export type CurrencyCode = "USD";

export type FinancialItemType = "DEBT" | "GOAL";

export type FinancialStatus = "ACTIVE" | "PARTIAL" | "COMPLETED";

export type StatusFilter = "all" | "active" | "completed";

export type TypeFilter = "all" | "goal" | "debt";

/** Ledger entry. A goal's saved amount is confirmed transfers to that goal minus its expenses. Amount in cents. */
export type TransactionKind = "DEPOSIT" | "GOAL_TRANSFER" | "EXPENSE";

export interface Transaction {
  id: string;
  kind: TransactionKind;
  amount: number;
  goalId?: string;
  debtId?: string;
  debtPaymentId?: string;
  createdAt: string;
  note?: string;
  category?: string;
  syncUpdatedAt?: number;
  deleted?: boolean;
}

/** One time money was given on a debt. Amount in USD cents. */
export interface DebtAdvance {
  id: string;
  amount: number;
  givenAt: string;
  note?: string;
  syncUpdatedAt?: number;
  deleted?: boolean;
}

/** Remaining principal: money given minus money returned. Never negative. */
export function debtRemainingCents(advanceCents: number[], paymentCents: number[]): number {
  const given = advanceCents.reduce((sum, cents) => sum + Math.max(cents, 0), 0);
  const returned = paymentCents.reduce((sum, cents) => sum + Math.max(cents, 0), 0);
  return Math.max(given - returned, 0);
}

/** Single payment / contribution entry for debts. Amount in USD cents. */
export interface Payment {
  id: string;
  amount: number;
  paidAt: string;
  affectsBalance?: boolean;
  currency?: string;
  syncUpdatedAt?: number;
  deleted?: boolean;
}

/**
 * Unified financial record.
 * - GOAL progress is derived from GOAL_TRANSFER transactions
 * - DEBT amount remains stored with payment history
 */
export interface FinancialItem {
  id: string;
  type: FinancialItemType;
  name: string;
  status: FinancialStatus;
  amount: number;
  target: number;
  history: Payment[];
  /** Disbursements. Missing on older records: the target is the first one, dated dateGiven. */
  advances?: DebtAdvance[];
  dateGiven?: string;
  dateDue?: string;
  /** ISO date (YYYY-MM-DD) — goal deadline for daily savings hint */
  dateDeadline?: string;
  /** MONEY is a savings target. ACTION is a task list. Older records omit this. */
  goalKind?: string;
  updatedAt?: string;
  /** Tombstone for LWW sync — hidden from UI when true */
  deleted?: boolean;
}

export interface FinanceState {
  currency: CurrencyCode;
  items: FinancialItem[];
  transactions: Transaction[];
}

export interface GlobalFinanceSummary {
  totalDepositsCents: number;
  totalGoalAllocatedCents: number;
  totalExpensesCents: number;
  /** Sum of accumulated across active (non-completed) goals. */
  totalAccumulatedCents: number;
  /** Sum of (target − accumulated) for active goals. */
  totalRemainingToGoalCents: number;
  /** Free wallet: deposits minus expenses that are not tied to a goal. Goal savings are separate. */
  globalBalanceCents: number;
  /** Same free wallet. A goal's balance does not reduce it. */
  availableBalanceCents: number;
}

export interface ActiveGoalMetrics {
  totalAccumulatedCents: number;
  totalRemainingToGoalCents: number;
}

export interface DashboardTotals {
  globalBalanceUsd: number;
  totalSavedUsd: number;
  totalRemainingToGoalUsd: number;
  totalDebtRemainingUsd: number;
  availableBalanceUsd: number;
  activeCount: number;
  completedCount: number;
}

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
  /** Free wallet: deposits minus expenses that are not tied to a goal. */
  globalBalanceCents: number;
  /** Free wallet available for expenses that are not tied to a goal. */
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

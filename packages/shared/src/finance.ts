export type CurrencyCode = "USD";

export type FinancialItemType = "DEBT" | "GOAL";

export type FinancialStatus = "ACTIVE" | "PARTIAL" | "COMPLETED";

export type StatusFilter = "all" | "active" | "completed";

export type TypeFilter = "all" | "goal" | "debt";

/** Ledger entry — source of truth for goals and global balance. Amount in USD cents. */
export type TransactionKind = "DEPOSIT" | "GOAL_TRANSFER" | "EXPENSE";

export interface Transaction {
  id: string;
  kind: TransactionKind;
  amount: number;
  goalId?: string;
  createdAt: string;
  note?: string;
  category?: string;
}

/** Single payment / contribution entry for debts. Amount in USD cents. */
export interface Payment {
  id: string;
  amount: number;
  paidAt: string;
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
  /** Income − expenses (deposits minus expenses). */
  globalBalanceCents: number;
  /** Unallocated pool for transfers and expenses. */
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

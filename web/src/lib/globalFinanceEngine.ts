import { createId } from "@/lib/money";
import type {
  ActiveGoalMetrics,
  FinanceState,
  FinancialItem,
  GlobalFinanceSummary,
  Transaction,
  TransactionKind,
} from "@/types/finance";
import { resolveStatus } from "@/lib/financeEngine";

export function goalAllocatedCents(transactions: Transaction[], goalId: string): number {
  return transactions
    .filter((tx) => !tx.deleted && tx.kind === "GOAL_TRANSFER" && tx.goalId === goalId)
    .reduce((sum, tx) => sum + tx.amount, 0);
}

export function goalSpentCents(transactions: Transaction[], goalId: string): number {
  return transactions
    .filter((tx) => !tx.deleted && tx.kind === "EXPENSE" && tx.goalId === goalId)
    .reduce((sum, tx) => sum + tx.amount, 0);
}

/** Saved on a goal after a manual expense taken from that goal. */
export function goalNetCents(transactions: Transaction[], goalId: string): number {
  return Math.max(goalAllocatedCents(transactions, goalId) - goalSpentCents(transactions, goalId), 0);
}

export function computeActiveGoalMetrics(items: FinancialItem[]): ActiveGoalMetrics {
  const activeGoals = items.filter(
    (item) =>
      item.type === "GOAL" &&
      item.goalKind !== "ACTION" &&
      !item.deleted &&
      item.status !== "COMPLETED",
  );

  return activeGoals.reduce(
    (acc, goal) => ({
      totalAccumulatedCents: acc.totalAccumulatedCents + goal.amount,
      totalRemainingToGoalCents:
        acc.totalRemainingToGoalCents + Math.max(goal.target - goal.amount, 0),
    }),
    { totalAccumulatedCents: 0, totalRemainingToGoalCents: 0 },
  );
}

export function computeGlobalFinanceSummary(state: FinanceState): GlobalFinanceSummary {
  const enriched = enrichFinanceState(state);
  const totalDepositsCents = state.transactions
    .filter((tx) => !tx.deleted && tx.kind === "DEPOSIT")
    .reduce((sum, tx) => sum + tx.amount, 0);

  const totalGoalAllocatedCents = state.transactions
    .filter((tx) => !tx.deleted && tx.kind === "GOAL_TRANSFER")
    .reduce((sum, tx) => sum + tx.amount, 0);

  const totalExpensesCents = state.transactions
    .filter((tx) => !tx.deleted && tx.kind === "EXPENSE" && !tx.goalId)
    .reduce((sum, tx) => sum + tx.amount, 0);

  const goalMetrics = computeActiveGoalMetrics(enriched.items);
  const availableBalanceCents = totalDepositsCents - totalExpensesCents;
  const globalBalanceCents = availableBalanceCents;

  return {
    totalDepositsCents,
    totalGoalAllocatedCents,
    totalExpensesCents,
    totalAccumulatedCents: goalMetrics.totalAccumulatedCents,
    totalRemainingToGoalCents: goalMetrics.totalRemainingToGoalCents,
    globalBalanceCents,
    availableBalanceCents,
  };
}

export function enrichGoalItem(item: FinancialItem, transactions: Transaction[]): FinancialItem {
  if (item.type !== "GOAL") return item;
  if (item.goalKind === "ACTION") return { ...item, amount: 0 };

  const amount = goalNetCents(transactions, item.id);
  return {
    ...item,
    amount,
    status: resolveStatus(item.target, amount),
  };
}

export function enrichFinanceState(state: FinanceState): FinanceState {
  return {
    ...state,
    items: state.items.map((item) =>
      item.deleted ? item : enrichGoalItem(item, state.transactions),
    ),
  };
}

export function addTransaction(
  state: FinanceState,
  input: {
    kind: TransactionKind;
    amountCents: number;
    goalId?: string;
    note?: string;
    category?: string;
  },
): { state: FinanceState; error?: string } {
  if (input.amountCents <= 0) {
    return { state, error: "Введите корректную сумму" };
  }

  const summary = computeGlobalFinanceSummary(state);

  if (input.kind === "EXPENSE" && input.goalId) {
    if (input.amountCents > goalNetCents(state.transactions, input.goalId)) {
      return { state, error: "Недостаточно средств на цели" };
    }
  } else if (input.kind === "EXPENSE" && input.amountCents > summary.availableBalanceCents) {
    return { state, error: "Недостаточно свободных средств" };
  }

  if (input.kind === "GOAL_TRANSFER" && !input.goalId) {
    return { state, error: "Цель не выбрана" };
  }

  const transaction: Transaction = {
    id: createId(),
    kind: input.kind,
    amount: input.amountCents,
    goalId: input.goalId,
    createdAt: new Date().toISOString(),
    note: input.note,
    category: input.category,
  };

  const next: FinanceState = {
    ...state,
    transactions: [transaction, ...state.transactions],
  };

  return { state: enrichFinanceState(next) };
}

export function migrateLegacyGoalHistory(state: FinanceState): FinanceState {
  if (state.transactions.length > 0) return state;

  const migrated: Transaction[] = [];

  for (const item of state.items) {
    if (item.type !== "GOAL") continue;

    for (const payment of item.history) {
      if (payment.deleted) continue;
      migrated.push({
        id: payment.id,
        kind: "GOAL_TRANSFER",
        amount: payment.amount,
        goalId: item.id,
        createdAt: payment.paidAt,
      });
    }

    if (item.history.every((payment) => payment.deleted) && item.amount > 0) {
      migrated.push({
        id: createId(),
        kind: "GOAL_TRANSFER",
        amount: item.amount,
        goalId: item.id,
        createdAt: item.updatedAt ?? new Date().toISOString(),
        note: "Миграция накоплений",
      });
    }
  }

  if (migrated.length === 0) return state;

  return enrichFinanceState({ ...state, transactions: migrated });
}

export function sortedTransactions(transactions: Transaction[]): Transaction[] {
  return transactions
    .filter((transaction) => !transaction.deleted)
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
}

import type { FinancialItem, Transaction } from "@/types/finance";

export function findOpenGoal(items: FinancialItem[], goalId: string | null): FinancialItem | null {
  if (!goalId) return null;
  return items.find((item) => item.id === goalId && item.type === "GOAL" && !item.deleted) ?? null;
}

export function goalDetailCapabilities(item: FinancialItem) {
  const taskGoal = item.goalKind === "ACTION";
  return {
    isTaskGoal: taskGoal,
    canTransfer: !taskGoal && item.status !== "COMPLETED",
    canExpense: !taskGoal,
  };
}

export function goalActivity(transactions: Transaction[], goalId: string): Transaction[] {
  return transactions.filter((tx) => {
    if (tx.deleted || tx.goalId !== goalId) return false;
    switch (tx.kind) {
      case "GOAL_TRANSFER":
      case "EXPENSE":
        return true;
      case "DEPOSIT":
        return false;
      default: {
        const _never: never = tx.kind;
        return _never;
      }
    }
  });
}

export function goalActivityLabel(kind: Transaction["kind"]): string {
  switch (kind) {
    case "GOAL_TRANSFER":
      return "Перевод";
    case "EXPENSE":
      return "Расход";
    case "DEPOSIT":
      return "Пополнение";
    default: {
      const _never: never = kind;
      return _never;
    }
  }
}

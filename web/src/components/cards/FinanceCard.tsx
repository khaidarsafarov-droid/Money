import { DebtCard } from "@/components/cards/DebtCard";
import { GoalCard } from "@/components/cards/GoalCard";
import type { FinancialItem } from "@/types/finance";

interface FinanceCardProps {
  item: FinancialItem;
  hideAmounts?: boolean;
  onAddPayment?: (item: FinancialItem, prefillCents?: number) => void;
  onAddAdvance?: (amountCents: number, givenAt: string, note: string) => void;
  onRecordPayment?: (amountCents: number, paidAt: string) => void;
  showHistory?: boolean;
  onOpenGoal?: (item: FinancialItem) => void;
  onGoalTransfer?: (item: FinancialItem) => void;
  onGoalExpense?: () => void;
  onDelete?: (item: FinancialItem) => void;
}

export function FinanceCard({
  item,
  hideAmounts = false,
  onAddPayment,
  onAddAdvance,
  onRecordPayment,
  showHistory = false,
  onOpenGoal,
  onGoalTransfer,
  onGoalExpense,
  onDelete,
}: FinanceCardProps) {
  if (item.type === "DEBT") {
    return (
      <DebtCard
        item={item}
        hideAmounts={hideAmounts}
        onAddPayment={onAddPayment}
        onAddAdvance={onAddAdvance}
        onRecordPayment={onRecordPayment}
        showHistory={showHistory}
        onDelete={onDelete}
      />
    );
  }
  return (
    <GoalCard
      item={item}
      hideAmounts={hideAmounts}
      onOpen={onOpenGoal}
      onTransfer={onGoalTransfer}
      onExpense={onGoalExpense}
      onDelete={onDelete}
    />
  );
}

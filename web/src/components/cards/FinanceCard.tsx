import { DebtCard } from "@/components/cards/DebtCard";
import { GoalCard } from "@/components/cards/GoalCard";
import type { FinancialItem } from "@/types/finance";

interface FinanceCardProps {
  item: FinancialItem;
  hideAmounts?: boolean;
  onAddPayment?: (item: FinancialItem, prefillCents?: number) => void;
  onGoalTransfer?: (item: FinancialItem) => void;
  onGoalExpense?: () => void;
  onDelete?: (item: FinancialItem) => void;
}

export function FinanceCard({
  item,
  hideAmounts = false,
  onAddPayment,
  onGoalTransfer,
  onGoalExpense,
  onDelete,
}: FinanceCardProps) {
  if (item.type === "DEBT") {
    return <DebtCard item={item} hideAmounts={hideAmounts} onAddPayment={onAddPayment} onDelete={onDelete} />;
  }
  return (
    <GoalCard
      item={item}
      hideAmounts={hideAmounts}
      onTransfer={onGoalTransfer}
      onExpense={onGoalExpense}
      onDelete={onDelete}
    />
  );
}

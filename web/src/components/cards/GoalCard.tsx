import { ArrowDownLeft, ArrowUpRight, Target, Wallet } from "lucide-react";
import { AppIcon } from "@/components/ui/SurfaceCard";
import { FinanceCardShell } from "@/components/cards/FinanceCardShell";
import type { FinancialItem } from "@/types/finance";

interface GoalCardProps {
  item: FinancialItem;
  hideAmounts?: boolean;
  onOpen?: (item: FinancialItem) => void;
  onTransfer?: (item: FinancialItem) => void;
  onExpense?: () => void;
  onDelete?: (item: FinancialItem) => void;
}

export function GoalCard({ item, hideAmounts = false, onOpen, onTransfer, onExpense, onDelete }: GoalCardProps) {
  const taskGoal = item.goalKind === "ACTION";
  const canTransfer = !taskGoal && item.status !== "COMPLETED" && onTransfer;
  const expense = taskGoal ? undefined : onExpense;

  return (
    <FinanceCardShell
      item={item}
      typeLabel={taskGoal ? "Задачи" : "Цель"}
      typeIcon={Target}
      remainingLabel="До цели"
      paidLabel="Накоплено"
      hideAmounts={hideAmounts}
      onOpen={onOpen}
      onDelete={onDelete}
      action={
        canTransfer || expense ? (
          <div className="flex w-full flex-col gap-2 sm:flex-row">
            {canTransfer ? (
              <button
                type="button"
                onClick={() => onTransfer(item)}
                className="pill-btn-secondary inline-flex flex-1 items-center justify-center gap-2"
              >
                <AppIcon icon={Wallet} size={14} />
                Перевод
              </button>
            ) : null}
            {expense ? (
              <button
                type="button"
                onClick={expense}
                className="pill-btn-ghost inline-flex flex-1 items-center justify-center gap-2 border border-outline-variant"
              >
                <AppIcon icon={ArrowUpRight} size={14} />
                Расход
              </button>
            ) : null}
          </div>
        ) : null
      }
    />
  );
}

export { ArrowDownLeft };

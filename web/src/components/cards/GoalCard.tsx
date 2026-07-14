import { ArrowDownLeft, ArrowUpRight, Target, Wallet } from "lucide-react";
import { AppIcon } from "@/components/ui/SurfaceCard";
import { FinanceCardShell } from "@/components/cards/FinanceCardShell";
import type { FinancialItem } from "@/types/finance";

interface GoalCardProps {
  item: FinancialItem;
  hideAmounts?: boolean;
  onTransfer?: (item: FinancialItem) => void;
  onExpense?: () => void;
  onDelete?: (item: FinancialItem) => void;
}

export function GoalCard({ item, hideAmounts = false, onTransfer, onExpense, onDelete }: GoalCardProps) {
  const canTransfer = item.status !== "COMPLETED" && onTransfer;

  return (
    <FinanceCardShell
      item={item}
      typeLabel="Цель"
      typeIcon={Target}
      remainingLabel="До цели"
      paidLabel="Накоплено"
      hideAmounts={hideAmounts}
      onDelete={onDelete}
      action={
        canTransfer || onExpense ? (
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
            {onExpense ? (
              <button
                type="button"
                onClick={onExpense}
                className="pill-btn-ghost inline-flex flex-1 items-center justify-center gap-2 border border-milk-surface"
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

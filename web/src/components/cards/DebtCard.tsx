import { HandCoins, Share2 } from "lucide-react";
import { isDebtOverdue } from "@/lib/financeEngine";
import { shareDebtReminder } from "@/lib/debtRemind";
import { AppIcon } from "@/components/ui/SurfaceCard";
import { FinanceCardShell } from "@/components/cards/FinanceCardShell";
import type { FinancialItem } from "@/types/finance";

interface DebtCardProps {
  item: FinancialItem;
  onAddPayment?: (item: FinancialItem) => void;
  onDelete?: (item: FinancialItem) => void;
}

export function DebtCard({ item, onAddPayment, onDelete }: DebtCardProps) {
  const canPay = item.status !== "COMPLETED" && onAddPayment;
  const overdue = isDebtOverdue(item);

  return (
    <FinanceCardShell
      item={item}
      typeLabel="Долг"
      typeIcon={HandCoins}
      remainingLabel="Осталось"
      paidLabel="Внесено"
      overdue={overdue}
      onDelete={onDelete}
      meta={
        item.dateDue ? (
          <p className={`text-sm ${overdue ? "font-medium text-milk-error" : "text-milk-muted"}`}>
            Срок: {new Date(item.dateDue).toLocaleDateString("ru-RU")}
          </p>
        ) : null
      }
      action={
        canPay ? (
          <div className="flex flex-col gap-2 sm:flex-row">
            <button
              type="button"
              onClick={() => onAddPayment(item)}
              className="pill-btn-primary inline-flex w-full items-center justify-center gap-2 sm:w-auto"
            >
              <AppIcon icon={HandCoins} size={18} />
              Добавить оплату
            </button>
            <button
              type="button"
              onClick={() => void shareDebtReminder(item)}
              className="pill-btn-ghost inline-flex w-full items-center justify-center gap-2 border border-milk-surface/80 sm:w-auto"
            >
              <AppIcon icon={Share2} size={18} />
              Напомнить
            </button>
          </div>
        ) : null
      }
    />
  );
}

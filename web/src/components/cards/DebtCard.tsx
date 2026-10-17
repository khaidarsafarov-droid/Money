import { HandCoins, MoreHorizontal, Share2, Zap } from "lucide-react";
import { useState } from "react";
import { isDebtOverdue } from "@/lib/financeEngine";
import { suggestAcceleratePaymentCents } from "@/lib/debtFreedomEngine";
import { shareDebtReminder } from "@/lib/debtRemind";
import { AppIcon } from "@/components/ui/SurfaceCard";
import { FinanceCardShell } from "@/components/cards/FinanceCardShell";
import type { FinancialItem } from "@/types/finance";

interface DebtCardProps {
  item: FinancialItem;
  hideAmounts?: boolean;
  onAddPayment?: (item: FinancialItem, prefillCents?: number) => void;
  onDelete?: (item: FinancialItem) => void;
}

export function DebtCard({ item, hideAmounts = false, onAddPayment, onDelete }: DebtCardProps) {
  const [menuOpen, setMenuOpen] = useState(false);
  const canPay = item.status !== "COMPLETED" && onAddPayment;
  const overdue = isDebtOverdue(item);
  const remaining = Math.max(item.target - item.amount, 0);
  const accelerateCents = suggestAcceleratePaymentCents(remaining, item.target, item.amount);

  return (
    <FinanceCardShell
      item={item}
      typeLabel="Долг"
      typeIcon={HandCoins}
      remainingLabel="Осталось"
      paidLabel="Внесено"
      overdue={overdue}
      hideAmounts={hideAmounts}
      onDelete={onDelete}
      primaryAmountCents={remaining}
      menu={
        <div className="relative">
          <button
            type="button"
            onClick={() => setMenuOpen((open) => !open)}
            className="flex min-h-11 min-w-11 items-center justify-center rounded-md text-milk-muted transition hover:bg-milk-bg"
            aria-label="Ещё"
          >
            <AppIcon icon={MoreHorizontal} size={18} />
          </button>
          {menuOpen ? (
            <>
              <button
                type="button"
                aria-label="Закрыть меню"
                className="fixed inset-0 z-10"
                onClick={() => setMenuOpen(false)}
              />
              <div className="absolute right-0 z-20 mt-1 min-w-40 rounded-md border border-milk-surface bg-milk-glass py-1 shadow-sm">
                {canPay ? (
                  <button
                    type="button"
                    className="flex w-full items-center gap-2 px-3 py-2.5 text-left text-sm hover:bg-milk-bg"
                    onClick={() => {
                      setMenuOpen(false);
                      onAddPayment?.(item, accelerateCents);
                    }}
                  >
                    <AppIcon icon={Zap} size={14} />
                    Ускорить
                  </button>
                ) : null}
                <button
                  type="button"
                  className="flex w-full items-center gap-2 px-3 py-2.5 text-left text-sm hover:bg-milk-bg"
                  onClick={() => {
                    setMenuOpen(false);
                    void shareDebtReminder(item);
                  }}
                >
                  <AppIcon icon={Share2} size={14} />
                  Напомнить
                </button>
              </div>
            </>
          ) : null}
        </div>
      }
      meta={
        item.dateDue ? (
          <p className={`text-subheading ${overdue ? "font-medium text-milk-error" : ""}`}>
            Срок: {new Date(item.dateDue).toLocaleDateString("ru-RU")}
          </p>
        ) : null
      }
      action={
        canPay ? (
          <button
            type="button"
            onClick={() => onAddPayment(item)}
            className="pill-btn-primary inline-flex w-full items-center justify-center gap-2"
          >
            <AppIcon icon={HandCoins} size={14} className="text-white" />
            Добавить оплату
          </button>
        ) : null
      }
    />
  );
}

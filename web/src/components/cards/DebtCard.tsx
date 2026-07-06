import { HandCoins, Share2, Zap } from "lucide-react";
import { isDebtOverdue } from "@/lib/financeEngine";
import {
  computeDebtMilestones,
  estimatePaymentsToClose,
  isSmallDebt,
  suggestAcceleratePaymentCents,
} from "@/lib/debtFreedomEngine";
import { shareDebtReminder } from "@/lib/debtRemind";
import { formatMoney } from "@/lib/money";
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
  const canPay = item.status !== "COMPLETED" && onAddPayment;
  const overdue = isDebtOverdue(item);
  const remaining = Math.max(item.target - item.amount, 0);
  const milestones = computeDebtMilestones(item.amount, item.target);
  const accelerateCents = suggestAcceleratePaymentCents(remaining, item.target, item.amount);
  const paymentsToClose = estimatePaymentsToClose(remaining, accelerateCents);

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
      meta={
        <div className="space-y-1">
          {item.dateDue ? (
            <p className={`text-sm ${overdue ? "font-medium text-milk-error" : "text-milk-muted"}`}>
              Срок: {new Date(item.dateDue).toLocaleDateString("ru-RU")}
            </p>
          ) : null}
          {item.status !== "COMPLETED" ? (
            <>
              <p className="text-sm text-milk-muted">
                Вы прошли {milestones.progressPercent}% пути к закрытию долга
              </p>
              {milestones.nextMilestone ? (
                <p className="text-sm font-medium text-milk-gold">
                  До вехи {milestones.nextMilestone.percent}% осталось{" "}
                  {formatMoney(milestones.nextMilestone.centsToReach, { hidden: hideAmounts })}
                </p>
              ) : null}
              {isSmallDebt(remaining) ? (
                <p className="text-sm text-milk-success">
                  Этот долг можно закрыть примерно за {paymentsToClose} платеж(а)
                </p>
              ) : null}
            </>
          ) : null}
        </div>
      }
      action={
        canPay ? (
          <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap">
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
              onClick={() => onAddPayment(item, accelerateCents)}
              className="pill-btn-ghost inline-flex w-full items-center justify-center gap-2 border border-milk-gold/40 text-milk-gold sm:w-auto"
            >
              <AppIcon icon={Zap} size={18} />
              Ускорить
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

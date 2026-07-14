import { AlertTriangle, History, Trash2, type LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { progressFraction, remainingAmount } from "@/lib/financeEngine";
import { formatMoney } from "@/lib/money";
import { AppIcon, SurfaceCard } from "@/components/ui/SurfaceCard";
import {
  AnimatedProgressBar,
  progressVariantForItem,
} from "@/components/ui/AnimatedProgressBar";
import type { FinancialItem, FinancialStatus } from "@/types/finance";
import {
  dailyGoalCents,
  displayProgressFraction,
  goalSurplusCents,
  isGoalDeadlinePassed,
  isGoalOverAchieved,
  progressPercentRaw,
} from "@/lib/goalProgress";

const statusLabels: Record<FinancialStatus, string> = {
  ACTIVE: "Активный",
  PARTIAL: "Частично",
  COMPLETED: "Завершён",
};

interface FinanceCardShellProps {
  item: FinancialItem;
  typeLabel: string;
  typeIcon: LucideIcon;
  remainingLabel: string;
  paidLabel: string;
  overdue?: boolean;
  hideAmounts?: boolean;
  onDelete?: (item: FinancialItem) => void;
  action?: ReactNode;
  meta?: ReactNode;
}

export function FinanceCardShell({
  item,
  typeLabel,
  typeIcon,
  remainingLabel,
  paidLabel,
  overdue = false,
  hideAmounts = false,
  onDelete,
  action,
  meta,
}: FinanceCardShellProps) {
  const remaining = remainingAmount(item);
  const progress = item.type === "GOAL" ? displayProgressFraction(item) : progressFraction(item);
  const progressPercent = progressPercentRaw(item);
  const overAchieved = item.type === "GOAL" && isGoalOverAchieved(item);
  const daily = item.type === "GOAL" ? dailyGoalCents(item) : null;

  return (
    <SurfaceCard
      className={`space-y-3 ${overdue ? "border-milk-error/40" : ""} ${overAchieved ? "border-milk-gold/40" : ""} ${item.type === "GOAL" && item.status !== "COMPLETED" ? "goal-card-active" : ""}`}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 space-y-0.5">
          <div className="flex flex-wrap items-center gap-2 text-milk-muted">
            <AppIcon icon={typeIcon} size={14} />
            <span className="text-xs">{typeLabel}</span>
            <span
              className={`text-xs ${
                item.status === "COMPLETED"
                  ? "text-milk-success"
                  : overdue
                    ? "text-milk-error"
                    : "text-milk-muted"
              }`}
            >
              · {overdue ? "Просрочен" : statusLabels[item.status]}
            </span>
          </div>
          <h3 className="truncate text-base font-medium text-milk-text">{item.name}</h3>
          {meta}
        </div>

        <div className="flex shrink-0 items-start gap-1">
          <span className="text-sm tabular-nums text-milk-muted">{progressPercent}%</span>
          {onDelete && (
            <button
              type="button"
              onClick={() => onDelete(item)}
              className="rounded p-1 text-milk-muted transition hover:bg-milk-bg hover:text-milk-error"
              aria-label="Удалить"
            >
              <AppIcon icon={Trash2} size={14} />
            </button>
          )}
        </div>
      </div>

      <AnimatedProgressBar
        value={progress}
        variant={progressVariantForItem(item, overdue)}
      />

      {overAchieved && (
        <p className="text-sm text-milk-gold">
          Перевыполнение: +{formatMoney(goalSurplusCents(item), { hidden: hideAmounts })}
        </p>
      )}

      {item.type === "GOAL" && item.dateDeadline && (
        <p className="text-sm text-milk-muted">
          Срок: {new Date(item.dateDeadline).toLocaleDateString("ru-RU")}
          {isGoalDeadlinePassed(item.dateDeadline) && item.status !== "COMPLETED" && (
            <span className="ml-1 text-milk-error">· срок прошёл</span>
          )}
        </p>
      )}

      {daily != null && (
        <p className="text-sm text-milk-muted">
          Ежедневно: <span className="text-milk-text">{formatMoney(daily, { hidden: hideAmounts })}</span>
        </p>
      )}

      {overdue && (
        <div className="flex items-center gap-1.5 text-sm text-milk-error">
          <AppIcon icon={AlertTriangle} size={14} className="text-milk-error" />
          Срок возврата прошёл — осталось {formatMoney(remaining, { hidden: hideAmounts })}
        </div>
      )}

      <p className="text-sm text-milk-muted">
        {remainingLabel}:{" "}
        <span className="text-milk-text">{formatMoney(remaining, { hidden: hideAmounts })}</span>
      </p>

      <p className="text-sm text-milk-muted">
        {paidLabel} {formatMoney(item.amount, { hidden: hideAmounts })} из {formatMoney(item.target, { hidden: hideAmounts })}
      </p>

      {action}

      {item.history.length > 0 && (
        <details className="border-t border-milk-surface pt-2">
          <summary className="flex cursor-pointer list-none items-center gap-1.5 text-xs text-milk-muted [&::-webkit-details-marker]:hidden">
            <AppIcon icon={History} size={12} />
            История ({item.history.length})
          </summary>
          <ul className="mt-2 space-y-1 text-sm">
            {item.history.map((payment) => (
              <li key={payment.id} className="flex justify-between gap-2">
                <span className="text-milk-muted">
                  {new Date(payment.paidAt).toLocaleDateString("ru-RU")}
                </span>
                <span className="text-milk-text">+{formatMoney(payment.amount, { hidden: hideAmounts })}</span>
              </li>
            ))}
          </ul>
        </details>
      )}
    </SurfaceCard>
  );
}

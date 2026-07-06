import { motion } from "framer-motion";
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
    <motion.div
      layout
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.98 }}
      transition={{ type: "spring", stiffness: 260, damping: 26 }}
    >
      <SurfaceCard
        className={`space-y-4 ${overdue ? "ring-1 ring-milk-error/25" : ""} ${overAchieved ? "ring-1 ring-milk-gold/40" : ""} ${item.type === "GOAL" && item.status !== "COMPLETED" ? "goal-card-active" : ""}`}
      >
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0 space-y-1">
            <div className="flex flex-wrap items-center gap-2 text-milk-muted">
              <AppIcon icon={typeIcon} size={16} />
              <span className="text-xs font-medium uppercase tracking-widest">{typeLabel}</span>
              <span
                className={`rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${
                  item.status === "COMPLETED"
                    ? "bg-milk-success/15 text-milk-success"
                    : overdue
                      ? "bg-milk-error/15 text-milk-error"
                      : "bg-milk-surface/90 text-milk-muted"
                }`}
              >
                {overdue ? "Просрочен" : statusLabels[item.status]}
              </span>
            </div>
            <h3 className="truncate text-lg font-semibold text-milk-text">{item.name}</h3>
            {meta}
          </div>

          <div className="flex shrink-0 items-start gap-2">
            <span className="rounded-full bg-milk-surface px-3 py-1 text-sm font-bold text-milk-text">
              {progressPercent}%
            </span>
            {onDelete && (
              <button
                type="button"
                onClick={() => onDelete(item)}
                className="rounded-full p-2 text-milk-muted transition hover:bg-milk-error/10 hover:text-milk-error"
                aria-label="Удалить"
              >
                <AppIcon icon={Trash2} size={18} />
              </button>
            )}
          </div>
        </div>

        <AnimatedProgressBar
          value={progress}
          variant={progressVariantForItem(item, overdue)}
        />

        {overAchieved && (
          <p className="text-sm font-semibold text-[#C9A24D]">
            Перевыполнение: +{formatMoney(goalSurplusCents(item), { hidden: hideAmounts })}
          </p>
        )}

        {item.type === "GOAL" && item.dateDeadline && (
          <p className="text-sm text-milk-muted">
            Срок: {new Date(item.dateDeadline).toLocaleDateString("ru-RU")}
            {isGoalDeadlinePassed(item.dateDeadline) && item.status !== "COMPLETED" && (
              <span className="ml-2 font-medium text-milk-error">· срок прошёл</span>
            )}
          </p>
        )}

        {daily != null && (
          <p className="text-sm text-milk-muted">
            Ежедневно: <span className="font-semibold text-milk-text">{formatMoney(daily, { hidden: hideAmounts })}</span>
          </p>
        )}

        {overdue && (
          <div className="flex items-center gap-2 rounded-2xl bg-milk-error/10 px-3 py-2 text-sm text-milk-error">
            <AppIcon icon={AlertTriangle} size={16} />
            Срок возврата прошёл — осталось {formatMoney(remaining, { hidden: hideAmounts })}
          </div>
        )}

        <p className="text-sm text-milk-muted">
          {remainingLabel}:{" "}
          <span className="font-semibold text-milk-text">{formatMoney(remaining, { hidden: hideAmounts })}</span>
        </p>

        <p className="text-sm text-milk-muted">
          {paidLabel} {formatMoney(item.amount, { hidden: hideAmounts })} из {formatMoney(item.target, { hidden: hideAmounts })}
        </p>

        {action}

        {item.history.length > 0 && (
          <details className="rounded-2xl bg-milk-surface/50 p-3">
            <summary className="flex cursor-pointer list-none items-center gap-2 text-xs font-medium uppercase tracking-wide text-milk-muted [&::-webkit-details-marker]:hidden">
              <AppIcon icon={History} size={14} />
              История ({item.history.length})
            </summary>
            <ul className="mt-3 space-y-1 text-sm">
              {item.history.map((payment) => (
                <li key={payment.id} className="flex justify-between gap-2">
                  <span className="text-milk-muted">
                    {new Date(payment.paidAt).toLocaleDateString("ru-RU")}
                  </span>
                  <span className="font-medium text-milk-text">+{formatMoney(payment.amount, { hidden: hideAmounts })}</span>
                </li>
              ))}
            </ul>
          </details>
        )}
      </SurfaceCard>
    </motion.div>
  );
}

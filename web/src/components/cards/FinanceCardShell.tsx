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
  daysUntilDeadline,
  displayProgressFraction,
  formatGoalDeadline,
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
  onOpen?: (item: FinancialItem) => void;
  onDelete?: (item: FinancialItem) => void;
  action?: ReactNode;
  meta?: ReactNode;
  menu?: ReactNode;
  primaryAmountCents?: number;
}

export function FinanceCardShell({
  item,
  typeLabel,
  typeIcon,
  remainingLabel,
  paidLabel,
  overdue = false,
  hideAmounts = false,
  onOpen,
  onDelete,
  action,
  meta,
  menu,
  primaryAmountCents,
}: FinanceCardShellProps) {
  const taskGoal = item.type === "GOAL" && item.goalKind === "ACTION";
  const remaining = remainingAmount(item);
  const displayAmount = primaryAmountCents ?? remaining;
  const progress = taskGoal
    ? 0
    : item.type === "GOAL"
      ? displayProgressFraction(item)
      : progressFraction(item);
  const progressPercent = progressPercentRaw(item);
  const overAchieved = !taskGoal && item.type === "GOAL" && isGoalOverAchieved(item);
  const daily = !taskGoal && item.type === "GOAL" ? dailyGoalCents(item) : null;
  const locale = "ru";
  const english = false;
  const daysLeft = item.type === "GOAL" && item.dateDeadline ? daysUntilDeadline(item.dateDeadline) : null;
  const visibleHistory = item.history.filter((payment) => !payment.deleted);

  return (
    <SurfaceCard
      className={`space-y-3 ${overdue ? "border-error/40" : ""} ${overAchieved ? "border-primary/40" : ""} ${item.type === "GOAL" && item.status !== "COMPLETED" ? "goal-card-active" : ""}`}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 space-y-1">
          <div className="flex flex-wrap items-center gap-2">
            <AppIcon icon={typeIcon} size={14} className="text-on-surface-variant" />
            <span className="text-subheading">{typeLabel}</span>
            <span
              className={`rounded px-2 py-0.5 text-xs font-medium ${
                item.status === "COMPLETED"
                  ? "bg-primary/15 text-primary"
                  : overdue
                    ? "bg-error-container text-on-error-container"
                    : "bg-background text-on-surface-variant"
              }`}
            >
              {overdue ? "Просрочен" : statusLabels[item.status]}
            </span>
          </div>
          {onOpen ? (
            <button
              type="button"
              onClick={() => onOpen(item)}
              className="block w-full truncate text-left text-heading focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
            >
              {item.name}
            </button>
          ) : (
            <h3 className="truncate text-heading">{item.name}</h3>
          )}
          {taskGoal ? (
            <p className="text-sm text-on-surface-variant">
              {english ? "Tasks, no money target" : "Задачи без денежной цели"}
            </p>
          ) : (
            <p className="text-display tabular-nums">
              {formatMoney(displayAmount, { hidden: hideAmounts })}
            </p>
          )}
          {meta}
        </div>

        <div className="flex shrink-0 items-start gap-1">
          {taskGoal ? null : (
            <span className="text-sm font-semibold tabular-nums text-on-surface-variant">{progressPercent}%</span>
          )}
          {menu}
          {onDelete && (
            <button
              type="button"
              onClick={() => onDelete(item)}
              className="flex min-h-12 min-w-12 items-center justify-center rounded-md text-on-surface-variant transition hover:bg-background hover:text-error focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
              aria-label="Удалить"
            >
              <AppIcon icon={Trash2} size={14} />
            </button>
          )}
        </div>
      </div>

      {taskGoal ? null : (
        <AnimatedProgressBar
          value={progress}
          variant={progressVariantForItem(item, overdue)}
        />
      )}

      {overAchieved && (
        <p className="text-sm text-on-surface">
          Перевыполнение: +{formatMoney(goalSurplusCents(item), { hidden: hideAmounts })}
        </p>
      )}

      {item.type === "GOAL" && item.dateDeadline && (
        <p className="text-sm text-on-surface-variant">
          {english ? "Deadline" : "Срок"}: {formatGoalDeadline(item.dateDeadline, locale)}
          {isGoalDeadlinePassed(item.dateDeadline) && item.status !== "COMPLETED" && (
            <span className="text-error">{english ? " · deadline passed" : " · срок прошёл"}</span>
          )}
        </p>
      )}

      {daily != null && daysLeft != null && (
        <p className="text-sm text-on-surface-variant">
          {english ? "Daily" : "Ежедневно"}:{" "}
          <span className="text-on-surface">{formatMoney(daily, { hidden: hideAmounts })}</span>
          {" · "}
          {english ? `${daysLeft} days left` : `осталось ${daysLeft} дн.`}
        </p>
      )}

      {overdue && (
        <div className="flex items-center gap-1.5 text-sm text-error">
          <AppIcon icon={AlertTriangle} size={14} className="text-error" />
          Срок возврата прошёл — осталось {formatMoney(remaining, { hidden: hideAmounts })}
        </div>
      )}

      {taskGoal ? null : (
        <>
          <p className="text-sm text-on-surface-variant">
            {remainingLabel}:{" "}
            <span className="text-on-surface">{formatMoney(remaining, { hidden: hideAmounts })}</span>
          </p>

          <p className="text-sm text-on-surface-variant">
            {paidLabel} {formatMoney(item.amount, { hidden: hideAmounts })} из {formatMoney(item.target, { hidden: hideAmounts })}
          </p>
        </>
      )}

      {action}

      {visibleHistory.length > 0 && (
        <details className="border-t border-outline-variant pt-2">
          <summary className="flex cursor-pointer list-none items-center gap-1.5 text-xs text-on-surface-variant [&::-webkit-details-marker]:hidden">
            <AppIcon icon={History} size={12} />
            История ({visibleHistory.length})
          </summary>
          <ul className="mt-2 space-y-1 text-sm">
            {visibleHistory.map((payment) => (
              <li key={payment.id} className="flex justify-between gap-2">
                <span className="text-on-surface-variant">
                  {new Date(payment.paidAt).toLocaleDateString("ru-RU")}
                </span>
                <span className="text-on-surface">+{formatMoney(payment.amount, { hidden: hideAmounts })}</span>
              </li>
            ))}
          </ul>
        </details>
      )}
    </SurfaceCard>
  );
}

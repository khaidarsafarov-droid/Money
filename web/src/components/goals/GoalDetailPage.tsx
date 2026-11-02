import { ArrowLeft, ArrowUpRight, Target, Trash2, Wallet } from "lucide-react";
import { AppIcon, SurfaceCard } from "@/components/ui/SurfaceCard";
import { AnimatedProgressBar, progressVariantForItem } from "@/components/ui/AnimatedProgressBar";
import { goalDetailCapabilities } from "@/lib/goalDetail";
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
import { remainingAmount } from "@/lib/financeEngine";
import { goalActivity, goalActivityLabel } from "@/lib/goalDetail";
import { formatMoney } from "@/lib/money";
import type { FinancialItem, Transaction } from "@/types/finance";

interface GoalDetailPageProps {
  item: FinancialItem;
  transactions?: Transaction[];
  hideAmounts?: boolean;
  onBack: () => void;
  onTransfer: () => void;
  onExpense: () => void;
  onDelete: () => void;
}

export function GoalDetailPage({
  item,
  transactions = [],
  hideAmounts = false,
  onBack,
  onTransfer,
  onExpense,
  onDelete,
}: GoalDetailPageProps) {
  const { isTaskGoal, canTransfer, canExpense } = goalDetailCapabilities(item);
  const remaining = remainingAmount(item);
  const progress = isTaskGoal ? 0 : displayProgressFraction(item);
  const progressPercent = progressPercentRaw(item);
  const overAchieved = !isTaskGoal && isGoalOverAchieved(item);
  const daily = !isTaskGoal ? dailyGoalCents(item) : null;
  const daysLeft = item.dateDeadline ? daysUntilDeadline(item.dateDeadline) : null;
  const overdue = Boolean(item.dateDeadline && isGoalDeadlinePassed(item.dateDeadline) && item.status !== "COMPLETED");
  const activity = goalActivity(transactions, item.id);

  return (
    <section className="space-y-5 pb-8" aria-label={`Цель ${item.name}`}>
      <button
        type="button"
        onClick={onBack}
        className="inline-flex items-center gap-2 text-sm font-semibold text-primary"
      >
        <AppIcon icon={ArrowLeft} size={16} className="text-primary" />
        Назад
      </button>

      <header className="flex items-start justify-between gap-3">
        <div className="min-w-0 space-y-1">
          <p className="text-subheading">{isTaskGoal ? "Задачи" : "Цель"}</p>
          <h1 className="text-2xl font-semibold tracking-tight text-on-surface">{item.name}</h1>
          <p className="text-sm text-on-surface-variant">
            {item.status === "COMPLETED" ? "Выполнена" : overdue ? "Просрочена" : "Активна"}
          </p>
        </div>
        <button
          type="button"
          onClick={onDelete}
          className="flex min-h-12 min-w-12 items-center justify-center rounded-md text-on-surface-variant transition hover:bg-background hover:text-error focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
          aria-label="Удалить"
        >
          <AppIcon icon={Trash2} size={16} />
        </button>
      </header>

      <SurfaceCard className="space-y-4">
        {isTaskGoal ? (
          <p className="text-sm text-on-surface-variant">Задачи без денежной цели</p>
        ) : (
          <>
            <div className="flex items-end justify-between gap-3">
              <p className="text-display tabular-nums">
                {formatMoney(item.amount, { hidden: hideAmounts })}
              </p>
              <p className="text-sm font-semibold tabular-nums text-on-surface-variant">
                {progressPercent}%
              </p>
            </div>
            <AnimatedProgressBar
              value={progress}
              variant={progressVariantForItem(item, overdue)}
            />
            <p className="text-sm text-on-surface-variant">
              Накоплено {formatMoney(item.amount, { hidden: hideAmounts })} из{" "}
              {formatMoney(item.target, { hidden: hideAmounts })}
            </p>
            <p className="text-sm text-on-surface-variant">
              До цели:{" "}
              <span className="text-on-surface">{formatMoney(remaining, { hidden: hideAmounts })}</span>
            </p>
          </>
        )}

        {overAchieved ? (
          <p className="text-sm text-on-surface">
            Перевыполнение: +{formatMoney(goalSurplusCents(item), { hidden: hideAmounts })}
          </p>
        ) : null}

        {item.dateDeadline ? (
          <p className="text-sm text-on-surface-variant">
            Срок: {formatGoalDeadline(item.dateDeadline, "ru")}
            {overdue ? <span className="text-error"> · срок прошёл</span> : null}
          </p>
        ) : null}

        {daily != null && daysLeft != null ? (
          <p className="text-sm text-on-surface-variant">
            Ежедневно:{" "}
            <span className="text-on-surface">{formatMoney(daily, { hidden: hideAmounts })}</span>
            {" · "}
            осталось {daysLeft} дн.
          </p>
        ) : null}

        {canTransfer || canExpense ? (
          <div className="flex w-full flex-col gap-2 sm:flex-row">
            {canTransfer ? (
              <button
                type="button"
                onClick={onTransfer}
                className="pill-btn-secondary inline-flex flex-1 items-center justify-center gap-2"
              >
                <AppIcon icon={Wallet} size={14} />
                Перевод
              </button>
            ) : null}
            {canExpense ? (
              <button
                type="button"
                onClick={onExpense}
                className="pill-btn-ghost inline-flex flex-1 items-center justify-center gap-2 border border-outline-variant"
              >
                <AppIcon icon={ArrowUpRight} size={14} />
                Расход
              </button>
            ) : null}
          </div>
        ) : null}
      </SurfaceCard>

      {activity.length > 0 ? (
        <SurfaceCard className="space-y-3">
          <h2 className="text-base font-semibold">История</h2>
          <ul className="space-y-2 text-sm">
            {activity.map((tx) => (
              <li key={tx.id} className="flex justify-between gap-2">
                <span className="text-on-surface-variant">
                  {goalActivityLabel(tx.kind)}
                  {" · "}
                  {new Date(tx.createdAt).toLocaleDateString("ru-RU")}
                </span>
                <span className="text-on-surface">
                  {tx.kind === "EXPENSE" ? "−" : "+"}
                  {formatMoney(tx.amount, { hidden: hideAmounts })}
                </span>
              </li>
            ))}
          </ul>
        </SurfaceCard>
      ) : (
        <SurfaceCard>
          <div className="flex items-center gap-2 text-sm text-on-surface-variant">
            <AppIcon icon={Target} size={14} />
            Пока нет записей по этой цели
          </div>
        </SurfaceCard>
      )}
    </section>
  );
}

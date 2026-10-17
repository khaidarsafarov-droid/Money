import { summarizeGoals } from "@/lib/goalProgress";
import { formatMoney } from "@/lib/money";
import { SurfaceCard } from "@/components/ui/SurfaceCard";
import type { FinancialItem } from "@/types/finance";

interface GoalSectionSummaryProps {
  items: FinancialItem[];
  hideAmounts?: boolean;
}

export function GoalSectionSummary({ items, hideAmounts = false }: GoalSectionSummaryProps) {
  const summary = summarizeGoals(items);
  if (summary.activeCount === 0) return null;

  const english = navigator.language.toLowerCase().startsWith("en");
  const saved = formatMoney(summary.savedCents, { hidden: hideAmounts });
  const target = formatMoney(summary.targetCents, { hidden: hideAmounts });

  return (
    <SurfaceCard className="space-y-2">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm text-on-surface">
          {english ? `Saved ${saved} of ${target}` : `Накоплено ${saved} из ${target}`}
        </p>
        <p className="text-base font-semibold tabular-nums text-on-surface">{summary.progressPercent}%</p>
      </div>
      <div className="h-2 overflow-hidden rounded-full bg-background">
        <div
          className="h-full rounded-full bg-primary"
          style={{ width: `${Math.min(summary.progressPercent, 100)}%` }}
        />
      </div>
      {summary.dailyCents != null ? (
        <p className="text-sm text-on-surface-variant">
          {english ? "Daily until deadlines" : "В день до сроков"}:{" "}
          <span className="text-on-surface">{formatMoney(summary.dailyCents, { hidden: hideAmounts })}</span>
        </p>
      ) : null}
      {summary.overdueCount > 0 ? (
        <p className="text-sm font-medium text-error">
          {english ? `Overdue: ${summary.overdueCount}` : `Просрочено: ${summary.overdueCount}`}
        </p>
      ) : null}
    </SurfaceCard>
  );
}

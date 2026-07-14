import { useState } from "react";
import { ChevronDown, ChevronUp } from "lucide-react";
import { formatMoney } from "@/lib/money";
import { AppIcon } from "@/components/ui/SurfaceCard";
import type { GlobalFinanceSummary } from "@/types/finance";

interface BalanceOverviewCardProps {
  finance: GlobalFinanceSummary;
  formatUsd: (amount: number) => string;
  hideAmounts?: boolean;
}

export function BalanceOverviewCard({ finance, formatUsd, hideAmounts = false }: BalanceOverviewCardProps) {
  const [expanded, setExpanded] = useState(false);
  const money = (cents: number) => formatMoney(cents, { hidden: hideAmounts });

  return (
    <section className="balance-card space-y-4">
      <div className="space-y-1">
        <p className="text-subheading">Доступный остаток</p>
        <p className="text-display tabular-nums">
          {hideAmounts ? "•••" : formatUsd(finance.availableBalanceCents / 100)}
        </p>
        <p className="text-subheading">
          Доходы − расходы: {hideAmounts ? "•••" : formatUsd(finance.globalBalanceCents / 100)}
        </p>
      </div>

      <div className="flex flex-wrap gap-x-6 gap-y-2 text-sm">
        <div>
          <p className="text-subheading">Пополнения</p>
          <p className="font-semibold tabular-nums text-finance-positive">
            {money(finance.totalDepositsCents)}
          </p>
        </div>
        <div>
          <p className="text-subheading">Расходы</p>
          <p className="font-semibold tabular-nums text-finance-negative">
            {money(finance.totalExpensesCents)}
          </p>
        </div>
      </div>

      <button
        type="button"
        onClick={() => setExpanded((value) => !value)}
        className="pill-btn-ghost mx-auto inline-flex items-center gap-1"
      >
        {expanded ? "Свернуть" : "Подробнее"}
        <AppIcon icon={expanded ? ChevronUp : ChevronDown} size={14} />
      </button>

      {expanded ? (
        <div className="grid gap-2 border-t border-milk-surface pt-4 sm:grid-cols-2">
          <div className="rounded-md border border-milk-surface px-3 py-2.5">
            <p className="text-subheading">Накоплено</p>
            <p className="mt-0.5 text-base font-semibold tabular-nums text-finance-positive">
              {money(finance.totalAccumulatedCents)}
            </p>
          </div>
          <div className="rounded-md border border-milk-surface px-3 py-2.5">
            <p className="text-subheading">До целей</p>
            <p className="mt-0.5 text-base font-semibold tabular-nums text-milk-text">
              {money(finance.totalRemainingToGoalCents)}
            </p>
          </div>
        </div>
      ) : null}
    </section>
  );
}

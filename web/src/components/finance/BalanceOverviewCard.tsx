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
      <div className="pointer-events-none absolute -right-8 -top-10 size-32 rounded-full bg-white/10" />
      <div className="relative space-y-1">
        <p className="text-sm font-medium text-white/75">Доступный остаток</p>
        <p className="text-4xl font-semibold tracking-tight tabular-nums text-white">
          {hideAmounts ? "•••" : formatUsd(finance.availableBalanceCents / 100)}
        </p>
        <p className="text-sm text-white/75">
          Доходы − расходы: {hideAmounts ? "•••" : formatUsd(finance.globalBalanceCents / 100)}
        </p>
      </div>

      <div className="relative flex flex-wrap gap-x-6 gap-y-2 text-sm">
        <div>
          <p className="text-white/70">Пополнения</p>
          <p className="font-semibold tabular-nums text-white">{money(finance.totalDepositsCents)}</p>
        </div>
        <div>
          <p className="text-white/70">Расходы</p>
          <p className="font-semibold tabular-nums text-white">{money(finance.totalExpensesCents)}</p>
        </div>
      </div>

      <button
        type="button"
        onClick={() => setExpanded((value) => !value)}
        className="relative mx-auto inline-flex items-center gap-1 rounded-full px-4 py-2 text-sm font-medium text-white/85 hover:bg-white/10"
      >
        {expanded ? "Свернуть" : "Подробнее"}
        <AppIcon icon={expanded ? ChevronUp : ChevronDown} size={14} className="text-white" />
      </button>

      {expanded ? (
        <div className="relative grid gap-2 border-t border-white/15 pt-4 sm:grid-cols-2">
          <div className="rounded-2xl bg-white/10 px-3 py-2.5">
            <p className="text-white/70">Накоплено</p>
            <p className="mt-0.5 text-base font-semibold tabular-nums text-white">
              {money(finance.totalAccumulatedCents)}
            </p>
          </div>
          <div className="rounded-2xl bg-white/10 px-3 py-2.5">
            <p className="text-white/70">До целей</p>
            <p className="mt-0.5 text-base font-semibold tabular-nums text-white">
              {money(finance.totalRemainingToGoalCents)}
            </p>
          </div>
        </div>
      ) : null}
    </section>
  );
}

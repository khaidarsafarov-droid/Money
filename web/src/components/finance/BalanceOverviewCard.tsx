import { Wallet } from "lucide-react";
import { AppIcon } from "@/components/ui/SurfaceCard";
import { formatMoney } from "@/lib/money";
import type { GlobalFinanceSummary } from "@/types/finance";

interface BalanceOverviewCardProps {
  finance: GlobalFinanceSummary;
  formatUsd: (amount: number) => string;
  hideAmounts?: boolean;
}

export function BalanceOverviewCard({ finance, formatUsd, hideAmounts = false }: BalanceOverviewCardProps) {
  const money = (cents: number) => formatMoney(cents, { hidden: hideAmounts });
  return (
    <section className="balance-card space-y-5">
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-1">
          <p className="text-xs font-medium uppercase tracking-[0.2em] text-milk-muted">
            Остаток
          </p>
          <p className="text-3xl font-semibold tabular-nums tracking-tight text-milk-text sm:text-4xl">
            {hideAmounts ? "•••" : formatUsd(finance.globalBalanceCents / 100)}
          </p>
          <p className="text-sm text-milk-muted">Доходы − расходы</p>
        </div>
        <span className="inline-flex size-11 items-center justify-center rounded-2xl bg-milk-surface/70 text-milk-text">
          <AppIcon icon={Wallet} size={22} />
        </span>
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <div className="rounded-2xl border border-milk-surface/70 bg-milk-bg/80 px-4 py-3">
          <p className="text-xs uppercase tracking-wide text-milk-muted">Накоплено</p>
          <p className="mt-1 text-lg font-semibold tabular-nums text-finance-positive">
            {money(finance.totalAccumulatedCents)}
          </p>
          <p className="text-xs text-milk-muted">По активным целям</p>
        </div>
        <div className="rounded-2xl border border-milk-surface/70 bg-milk-bg/80 px-4 py-3">
          <p className="text-xs uppercase tracking-wide text-milk-muted">До целей</p>
          <p className="mt-1 text-lg font-semibold tabular-nums text-milk-text">
            {money(finance.totalRemainingToGoalCents)}
          </p>
          <p className="text-xs text-milk-muted">Осталось до 100%</p>
        </div>
      </div>

      <p className="text-sm leading-relaxed text-milk-muted">
        Чтобы закрыть все активные цели, добавьте ещё{" "}
        <span className="font-semibold text-milk-text">
          {money(finance.totalRemainingToGoalCents)}
        </span>
        .
      </p>

      <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm">
        <span>
          Пополнения{" "}
          <span className="font-semibold tabular-nums text-finance-positive">
            {money(finance.totalDepositsCents)}
          </span>
        </span>
        <span>
          Расходы{" "}
          <span className="font-semibold tabular-nums text-finance-negative">
            {money(finance.totalExpensesCents)}
          </span>
        </span>
      </div>
    </section>
  );
}

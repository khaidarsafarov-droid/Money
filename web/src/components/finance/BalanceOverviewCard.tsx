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
    <section className="balance-card space-y-4">
      <div className="space-y-0.5">
        <p className="text-xs text-milk-muted">Остаток</p>
        <p className="text-2xl font-medium tabular-nums tracking-tight text-milk-text sm:text-3xl">
          {hideAmounts ? "•••" : formatUsd(finance.globalBalanceCents / 100)}
        </p>
        <p className="text-sm text-milk-muted">Доходы − расходы</p>
      </div>

      <div className="grid gap-2 sm:grid-cols-2">
        <div className="rounded-md border border-milk-surface px-3 py-2.5">
          <p className="text-xs text-milk-muted">Накоплено</p>
          <p className="mt-0.5 text-base font-medium tabular-nums text-finance-positive">
            {money(finance.totalAccumulatedCents)}
          </p>
        </div>
        <div className="rounded-md border border-milk-surface px-3 py-2.5">
          <p className="text-xs text-milk-muted">До целей</p>
          <p className="mt-0.5 text-base font-medium tabular-nums text-milk-text">
            {money(finance.totalRemainingToGoalCents)}
          </p>
        </div>
      </div>

      <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-milk-muted">
        <span>
          Пополнения{" "}
          <span className="font-medium tabular-nums text-finance-positive">
            {money(finance.totalDepositsCents)}
          </span>
        </span>
        <span>
          Расходы{" "}
          <span className="font-medium tabular-nums text-finance-negative">
            {money(finance.totalExpensesCents)}
          </span>
        </span>
      </div>
    </section>
  );
}

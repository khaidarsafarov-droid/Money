import { useMemo, useState } from "react";
import { projectDebtFreedom } from "@/lib/debtFreedomEngine";
import { formatMoney } from "@/lib/money";
import { SurfaceCard } from "@/components/ui/SurfaceCard";
import type { FinancialItem } from "@/types/finance";

interface FreedomForecastCardProps {
  debts: FinancialItem[];
  hideAmounts?: boolean;
}

export function FreedomForecastCard({ debts, hideAmounts = false }: FreedomForecastCardProps) {
  const [extraMonthlyCents, setExtraMonthlyCents] = useState(0);
  const projection = useMemo(
    () => projectDebtFreedom(debts, extraMonthlyCents),
    [debts, extraMonthlyCents],
  );

  if (!projection.hasActiveDebts) {
    return (
      <SurfaceCard className="space-y-1">
        <p className="text-xs text-on-surface-variant">Свобода</p>
        <p className="text-sm text-on-surface-variant">Нет активных долгов.</p>
      </SurfaceCard>
    );
  }

  const freedomLabel = projection.freedomDateMs
    ? new Date(projection.freedomDateMs).toLocaleDateString("ru-RU", {
        day: "numeric",
        month: "long",
        year: "numeric",
      })
    : "—";

  return (
    <SurfaceCard className="space-y-3">
      <p className="text-xs text-on-surface-variant">Прогноз</p>

      <div>
        <p className="text-sm text-on-surface-variant">Дата свободы от долгов</p>
        <p className="text-xl font-medium text-on-surface">{freedomLabel}</p>
      </div>

      <p className="text-sm text-on-surface-variant">
        Осталось {formatMoney(projection.totalRemainingCents, { hidden: hideAmounts })} ·{" "}
        {formatMoney(projection.monthlyPaymentCents, { hidden: hideAmounts })}/мес · ~{projection.monthsRemaining} мес.
      </p>

      <label className="block space-y-1.5">
        <span className="text-sm text-on-surface">
          Дополнительно в месяц: {formatMoney(extraMonthlyCents, { hidden: hideAmounts })}
        </span>
        <input
          type="range"
          min={0}
          max={50000}
          step={2500}
          value={extraMonthlyCents}
          onChange={(event) => setExtraMonthlyCents(Number(event.target.value))}
          className="w-full accent-on-surface"
        />
      </label>

      {projection.monthsSavedWithExtra != null && projection.monthsSavedWithExtra > 0 ? (
        <p className="text-sm text-primary">
          С этой суммой вы освободитесь на {projection.monthsSavedWithExtra} мес. раньше.
        </p>
      ) : null}
    </SurfaceCard>
  );
}

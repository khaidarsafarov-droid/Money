import { useMemo, useState } from "react";
import { CalendarHeart } from "lucide-react";
import { projectDebtFreedom } from "@/lib/debtFreedomEngine";
import { formatMoney } from "@/lib/money";
import { AppIcon, SurfaceCard } from "@/components/ui/SurfaceCard";
import type { FinancialItem } from "@/types/finance";

interface FreedomForecastCardProps {
  debts: FinancialItem[];
}

export function FreedomForecastCard({ debts }: FreedomForecastCardProps) {
  const [extraMonthlyCents, setExtraMonthlyCents] = useState(0);
  const projection = useMemo(
    () => projectDebtFreedom(debts, extraMonthlyCents),
    [debts, extraMonthlyCents],
  );

  if (!projection.hasActiveDebts) {
    return (
      <SurfaceCard className="space-y-2">
        <div className="flex items-center gap-2 text-milk-muted">
          <AppIcon icon={CalendarHeart} size={18} />
          <span className="text-xs font-medium uppercase tracking-widest">Свобода</span>
        </div>
        <p className="text-sm text-milk-muted">Нет активных долгов — вы на финишной прямой.</p>
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
    <SurfaceCard className="space-y-4">
      <div className="flex items-center gap-2 text-milk-muted">
        <AppIcon icon={CalendarHeart} size={18} />
        <span className="text-xs font-medium uppercase tracking-widest">Просмотр будущего</span>
      </div>

      <div>
        <p className="text-sm text-milk-muted">Дата свободы от долгов</p>
        <p className="text-2xl font-semibold text-milk-gold">{freedomLabel}</p>
      </div>

      <p className="text-sm text-milk-muted">
        Осталось {formatMoney(projection.totalRemainingCents)} · темп{" "}
        {formatMoney(projection.monthlyPaymentCents)}/мес · ~{projection.monthsRemaining} мес.
      </p>

      <label className="block space-y-2">
        <span className="text-sm font-medium text-milk-text">
          Дополнительно в месяц: {formatMoney(extraMonthlyCents)}
        </span>
        <input
          type="range"
          min={0}
          max={50000}
          step={2500}
          value={extraMonthlyCents}
          onChange={(event) => setExtraMonthlyCents(Number(event.target.value))}
          className="w-full accent-milk-gold"
        />
      </label>

      {projection.monthsSavedWithExtra != null && projection.monthsSavedWithExtra > 0 ? (
        <p className="text-sm font-medium text-milk-success">
          С этой суммой вы освободитесь на {projection.monthsSavedWithExtra} мес. раньше.
        </p>
      ) : null}

      <p className="text-xs text-milk-muted">
        Прогноз строится по вашим платежам и остаткам — без банков и облака.
      </p>
    </SurfaceCard>
  );
}

import type { AutopilotSettings, PrivacyPreferences } from "@money/shared";
import { formatMoney } from "@/lib/money";
import { SurfaceCard } from "@/components/ui/SurfaceCard";

interface PsychologySettingsPanelProps {
  privacy: PrivacyPreferences;
  autopilot: AutopilotSettings;
  onPrivacyChange: (patch: Partial<PrivacyPreferences>) => void;
  onAutopilotChange: (patch: Partial<AutopilotSettings>) => void;
  hideAmounts: boolean;
}

export function PsychologySettingsPanel({
  privacy,
  autopilot,
  onPrivacyChange,
  onAutopilotChange,
  hideAmounts,
}: PsychologySettingsPanelProps) {
  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <SurfaceCard className="space-y-4">
        <div>
          <h3 className="text-sm font-semibold uppercase tracking-widest text-milk-muted">
            Приватность
          </h3>
          <p className="mt-1 text-sm text-milk-muted">
            Скрывайте суммы — вместо цифр показывается •••
          </p>
        </div>

        <label className="flex items-start gap-3 text-sm">
          <input
            type="checkbox"
            checked={privacy.hideAmounts}
            onChange={(event) => onPrivacyChange({ hideAmounts: event.target.checked })}
            className="mt-1 accent-milk-gold"
          />
          <span>
            <span className="font-medium text-milk-text">Скрывать суммы в веб-приложении</span>
            <span className="mt-1 block text-milk-muted">
              Пример: {formatMoney(12_500, { hidden: privacy.hideAmounts })}
            </span>
          </span>
        </label>
      </SurfaceCard>

      <SurfaceCard className="space-y-4">
        <div>
          <h3 className="text-sm font-semibold uppercase tracking-widest text-milk-muted">
            Автопилот
          </h3>
          <p className="mt-1 text-sm text-milk-muted">
            Ежедневный микроплатёж в выбранный долг одной кнопкой.
          </p>
        </div>

        <label className="flex items-center gap-3 text-sm">
          <input
            type="checkbox"
            checked={autopilot.enabled}
            onChange={(event) => onAutopilotChange({ enabled: event.target.checked })}
            className="accent-milk-gold"
          />
          <span className="font-medium text-milk-text">Включить автопилот</span>
        </label>

        <label className="block space-y-2 text-sm">
          <span className="font-medium text-milk-text">
            Сумма в день: {formatMoney(autopilot.dailyAmountCents, { hidden: hideAmounts })}
          </span>
          <input
            type="range"
            min={100}
            max={5000}
            step={100}
            value={autopilot.dailyAmountCents}
            onChange={(event) =>
              onAutopilotChange({ dailyAmountCents: Number(event.target.value) })
            }
            className="w-full accent-milk-gold"
          />
        </label>

        <div className="flex flex-wrap gap-2">
          {(["snowball", "avalanche"] as const).map((strategy) => (
            <button
              key={strategy}
              type="button"
              onClick={() => onAutopilotChange({ strategy })}
              className={
                autopilot.strategy === strategy ? "pill-btn-primary text-sm" : "pill-btn-secondary text-sm"
              }
            >
              {strategy === "snowball" ? "Снежный ком" : "Лавина"}
            </button>
          ))}
        </div>
      </SurfaceCard>
    </div>
  );
}

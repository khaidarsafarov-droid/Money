import type { LucideIcon } from "lucide-react";
import { AppIcon } from "@/components/ui/SurfaceCard";

type StatTone = "mint" | "peach" | "lilac" | "sand";

interface StatCardProps {
  label: string;
  value: string;
  icon: LucideIcon;
  hint?: string;
  tone?: StatTone;
}

const toneClass: Record<StatTone, string> = {
  mint: "bg-primary-container",
  peach: "bg-error-container",
  lilac: "bg-tertiary-container",
  sand: "bg-secondary-container",
};

export function StatCard({ label, value, icon, hint, tone = "mint" }: StatCardProps) {
  return (
    <section className={`space-y-2 rounded-[24px] p-4 ${toneClass[tone]}`}>
      <div className="flex items-center gap-2 text-on-surface-variant">
        <span className="flex size-7 items-center justify-center rounded-full bg-surface-container-lowest/80">
          <AppIcon icon={icon} size={14} className="text-primary" />
        </span>
        <p className="text-xs font-medium">{label}</p>
      </div>
      <p className="text-lg font-semibold tabular-nums text-on-surface">{value}</p>
      {hint ? <p className="text-xs text-on-surface-variant">{hint}</p> : null}
    </section>
  );
}

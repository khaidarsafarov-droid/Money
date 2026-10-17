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
  mint: "bg-pastel-mint",
  peach: "bg-pastel-peach",
  lilac: "bg-pastel-lilac",
  sand: "bg-pastel-sand",
};

export function StatCard({ label, value, icon, hint, tone = "mint" }: StatCardProps) {
  return (
    <section className={`space-y-2 rounded-[24px] p-4 ${toneClass[tone]}`}>
      <div className="flex items-center gap-2 text-[#5c7d72]">
        <span className="flex size-7 items-center justify-center rounded-full bg-white/80">
          <AppIcon icon={icon} size={14} className="text-milk-accent" />
        </span>
        <p className="text-xs font-medium">{label}</p>
      </div>
      <p className="text-lg font-semibold tabular-nums text-[#12352c]">{value}</p>
      {hint ? <p className="text-xs text-[#5c7d72]">{hint}</p> : null}
    </section>
  );
}

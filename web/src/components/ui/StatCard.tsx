import type { LucideIcon } from "lucide-react";
import { AppIcon, SurfaceCard } from "@/components/ui/SurfaceCard";

interface StatCardProps {
  label: string;
  value: string;
  icon: LucideIcon;
  hint?: string;
}

export function StatCard({ label, value, icon, hint }: StatCardProps) {
  return (
    <SurfaceCard className="relative overflow-hidden p-5">
      <div className="absolute -right-3 -top-3 opacity-[0.07]">
        <AppIcon icon={icon} size={72} />
      </div>
      <div className="relative space-y-1">
        <div className="flex items-center gap-2 text-milk-muted">
          <AppIcon icon={icon} size={16} />
          <p className="text-xs uppercase tracking-widest">{label}</p>
        </div>
        <p className="text-2xl font-semibold text-milk-text">{value}</p>
        {hint && <p className="text-xs text-milk-muted">{hint}</p>}
      </div>
    </SurfaceCard>
  );
}

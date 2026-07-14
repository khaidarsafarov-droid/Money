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
    <SurfaceCard className="space-y-2">
      <div className="flex items-center gap-1.5 text-milk-muted">
        <AppIcon icon={icon} size={14} />
        <p className="text-xs">{label}</p>
      </div>
      <p className="text-xl font-medium tabular-nums text-milk-text">{value}</p>
      {hint && <p className="text-xs text-milk-muted">{hint}</p>}
    </SurfaceCard>
  );
}

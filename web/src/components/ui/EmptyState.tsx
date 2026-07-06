import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { AppIcon, SurfaceCard } from "@/components/ui/SurfaceCard";

interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description: string;
  action?: ReactNode;
}

export function EmptyState({ icon, title, description, action }: EmptyStateProps) {
  return (
    <SurfaceCard className="flex flex-col items-center px-6 py-16 text-center">
      <div className="mb-4 rounded-full bg-milk-surface/70 p-4 text-milk-muted">
        <AppIcon icon={icon} size={28} />
      </div>
      <h3 className="text-lg font-semibold text-milk-text">{title}</h3>
      <p className="mt-2 max-w-sm text-sm text-milk-muted">{description}</p>
      {action && <div className="mt-6 flex flex-wrap justify-center gap-3">{action}</div>}
    </SurfaceCard>
  );
}

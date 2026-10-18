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
    <SurfaceCard className="flex flex-col items-center px-4 py-12 text-center">
      <AppIcon icon={icon} size={20} className="mb-3 text-on-surface-variant" />
      <h3 className="text-base font-medium text-on-surface">{title}</h3>
      <p className="mt-1 max-w-sm text-sm text-on-surface-variant">{description}</p>
      {action && <div className="mt-5 flex flex-wrap justify-center gap-2">{action}</div>}
    </SurfaceCard>
  );
}

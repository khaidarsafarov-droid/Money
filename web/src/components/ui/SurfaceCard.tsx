import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";

interface IconProps {
  icon: LucideIcon;
  className?: string;
  size?: number;
}

export function AppIcon({ icon: Icon, className = "", size = 20 }: IconProps) {
  return <Icon className={className} size={size} strokeWidth={1.5} />;
}

interface SurfaceCardProps {
  children: ReactNode;
  className?: string;
}

export function SurfaceCard({ children, className = "" }: SurfaceCardProps) {
  return (
    <section className={`glass-card p-6 ${className}`}>
      {children}
    </section>
  );
}

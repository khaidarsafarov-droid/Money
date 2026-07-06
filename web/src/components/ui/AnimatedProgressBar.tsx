import { motion } from "framer-motion";
import { isGoalOverAchieved } from "@/lib/goalProgress";
import type { FinancialItem } from "@/types/finance";

type ProgressVariant = "default" | "success" | "warning" | "danger" | "gold";

interface AnimatedProgressBarProps {
  value: number;
  className?: string;
  variant?: ProgressVariant;
}

const variantClasses: Record<ProgressVariant, string> = {
  default: "bg-milk-accent-dark",
  success: "bg-milk-success",
  warning: "bg-milk-warning",
  danger: "bg-milk-error",
  gold: "bg-[#C9A24D]",
};

export function AnimatedProgressBar({
  value,
  className = "",
  variant = "default",
}: AnimatedProgressBarProps) {
  const clamped = Math.min(Math.max(value, 0), 1);

  return (
    <div
      className={`h-2.5 overflow-hidden rounded-full bg-milk-surface/80 ${className}`}
      role="progressbar"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(clamped * 100)}
    >
      <motion.div
        className={`h-full rounded-full ${variantClasses[variant]}`}
        initial={false}
        animate={{ width: `${clamped * 100}%` }}
        transition={{ type: "spring", stiffness: 120, damping: 22 }}
      />
    </div>
  );
}

export function progressVariantForItem(
  item: FinancialItem,
  overdue: boolean,
): ProgressVariant {
  if (item.type === "GOAL" && isGoalOverAchieved(item)) return "gold";
  if (item.status === "COMPLETED") return "success";
  if (overdue) return "danger";
  if (item.status === "PARTIAL") return "warning";
  return "default";
}

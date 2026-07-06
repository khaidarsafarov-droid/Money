import { motion } from "framer-motion";
import type { StatusFilter } from "@/types/finance";

const filters: { id: StatusFilter; label: string }[] = [
  { id: "all", label: "Все" },
  { id: "active", label: "Активные" },
  { id: "completed", label: "Завершённые" },
];

interface StatusFilterTabsProps {
  value: StatusFilter;
  onChange: (filter: StatusFilter) => void;
}

export function StatusFilterTabs({ value, onChange }: StatusFilterTabsProps) {
  return (
    <div
      className="flex gap-1 rounded-full border border-milk-surface/80 bg-milk-glass p-1 backdrop-blur-sm"
      role="tablist"
      aria-label="Фильтр по статусу"
    >
      {filters.map((filter) => {
        const selected = value === filter.id;
        return (
          <button
            key={filter.id}
            type="button"
            role="tab"
            aria-selected={selected}
            onClick={() => onChange(filter.id)}
            className={`relative rounded-full px-4 py-2 text-sm font-medium transition-colors ${
              selected ? "text-milk-bg" : "text-milk-muted hover:text-milk-text"
            }`}
          >
            {selected && (
              <motion.span
                layoutId="status-filter-pill"
                className="absolute inset-0 rounded-full bg-milk-text"
                transition={{ type: "spring", stiffness: 380, damping: 32 }}
              />
            )}
            <span className="relative z-10">{filter.label}</span>
          </button>
        );
      })}
    </div>
  );
}

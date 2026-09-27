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
    <div className="flex gap-1 rounded-full bg-white p-1" role="tablist" aria-label="Фильтр по статусу">
      {filters.map((filter) => {
        const selected = value === filter.id;
        return (
          <button
            key={filter.id}
            type="button"
            role="tab"
            aria-selected={selected}
            onClick={() => onChange(filter.id)}
            className={`rounded-full px-3 py-2 text-sm transition-colors ${
              selected
                ? "bg-milk-accent font-semibold text-white"
                : "text-milk-muted hover:bg-pastel-mint hover:text-milk-text"
            }`}
          >
            {filter.label}
          </button>
        );
      })}
    </div>
  );
}

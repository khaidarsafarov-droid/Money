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
      className="flex gap-0 border-b border-milk-surface"
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
            className={`border-b-2 px-3 py-2.5 text-sm transition-colors ${
              selected
                ? "border-milk-gold bg-milk-bg font-semibold text-milk-gold"
                : "border-transparent text-milk-muted hover:bg-milk-bg hover:text-milk-text"
            }`}
          >
            {filter.label}
          </button>
        );
      })}
    </div>
  );
}

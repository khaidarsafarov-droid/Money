import type { TypeFilter } from "@/types/finance";

const filters: { id: TypeFilter; label: string }[] = [
  { id: "all", label: "Все типы" },
  { id: "goal", label: "Цели" },
  { id: "debt", label: "Долги" },
];

interface TypeFilterChipsProps {
  value: TypeFilter;
  onChange: (filter: TypeFilter) => void;
}

export function TypeFilterChips({ value, onChange }: TypeFilterChipsProps) {
  return (
    <div className="flex flex-wrap gap-2">
      {filters.map((filter) => {
        const selected = value === filter.id;
        return (
          <button
            key={filter.id}
            type="button"
            onClick={() => onChange(filter.id)}
            className={
              selected
                ? "pill-btn-secondary text-milk-text ring-1 ring-milk-accent/30"
                : "pill-btn-ghost border border-transparent"
            }
          >
            {filter.label}
          </button>
        );
      })}
    </div>
  );
}

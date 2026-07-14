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
    <div className="flex flex-wrap gap-1.5">
      {filters.map((filter) => {
        const selected = value === filter.id;
        return (
          <button
            key={filter.id}
            type="button"
            onClick={() => onChange(filter.id)}
            className={
              selected
                ? "pill-btn-secondary text-milk-text"
                : "pill-btn-ghost"
            }
          >
            {filter.label}
          </button>
        );
      })}
    </div>
  );
}

import { ArrowDownLeft, ArrowUpRight, HandCoins, Plus, Target, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { AppIcon } from "@/components/ui/SurfaceCard";

interface ExpandableFabProps {
  onAddGoal: () => void;
  onAddDebt: () => void;
  onAddDeposit: () => void;
  onAddExpense: () => void;
}

export function ExpandableFab({
  onAddGoal,
  onAddDebt,
  onAddDeposit,
  onAddExpense,
}: ExpandableFabProps) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    function handlePointerDown(event: MouseEvent) {
      if (!rootRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handlePointerDown);
    return () => document.removeEventListener("mousedown", handlePointerDown);
  }, [open]);

  const actions = [
    { label: "Пополнить", icon: ArrowDownLeft, onClick: onAddDeposit },
    { label: "Расход", icon: ArrowUpRight, onClick: onAddExpense },
    { label: "Новый долг", icon: HandCoins, onClick: onAddDebt },
    { label: "Новая цель", icon: Target, onClick: onAddGoal },
  ];

  return (
    <div ref={rootRef} className="fixed bottom-5 right-5 z-40 flex flex-col items-end gap-2">
      {open &&
        actions.map((action) => (
          <button
            key={action.label}
            type="button"
            onClick={() => {
              action.onClick();
              setOpen(false);
            }}
            className="inline-flex items-center gap-2 rounded-md border border-milk-surface bg-white px-3 py-2 text-sm text-milk-text"
          >
            <AppIcon icon={action.icon} size={14} />
            {action.label}
          </button>
        ))}

      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        aria-expanded={open}
        aria-label={open ? "Закрыть меню" : "Добавить запись"}
        className="inline-flex size-11 items-center justify-center rounded-md bg-milk-text text-white"
      >
        <AppIcon icon={open ? X : Plus} size={18} className="text-white" />
      </button>
    </div>
  );
}

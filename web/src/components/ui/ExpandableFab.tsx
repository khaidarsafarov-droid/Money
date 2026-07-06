import { AnimatePresence, motion } from "framer-motion";
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
    <div ref={rootRef} className="fixed bottom-6 right-6 z-40 flex flex-col items-end gap-3">
      <AnimatePresence>
        {open &&
          actions.map((action, index) => (
            <motion.button
              key={action.label}
              type="button"
              initial={{ opacity: 0, y: 12, scale: 0.9 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 8, scale: 0.95 }}
              transition={{
                type: "spring",
                stiffness: 420,
                damping: 28,
                delay: index * 0.04,
              }}
              onClick={() => {
                action.onClick();
                setOpen(false);
              }}
              className="inline-flex items-center gap-2 rounded-full border border-milk-surface bg-milk-bg/95 px-5 py-3 text-sm font-medium text-milk-text shadow-[0_8px_24px_rgb(45_42_38_/_0.12)] backdrop-blur-sm"
            >
              <AppIcon icon={action.icon} size={18} />
              {action.label}
            </motion.button>
          ))}
      </AnimatePresence>

      <motion.button
        type="button"
        whileHover={{ scale: 1.04 }}
        whileTap={{ scale: 0.96 }}
        onClick={() => setOpen((value) => !value)}
        aria-expanded={open}
        aria-label={open ? "Закрыть меню" : "Добавить запись"}
        className="inline-flex size-14 items-center justify-center rounded-full bg-milk-text text-milk-bg shadow-[0_12px_40px_rgb(45_42_38_/_0.25)]"
      >
        <motion.span animate={{ rotate: open ? 45 : 0 }} transition={{ duration: 0.2 }}>
          <AppIcon icon={open ? X : Plus} size={22} />
        </motion.span>
      </motion.button>
    </div>
  );
}

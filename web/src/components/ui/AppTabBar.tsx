import { HandCoins, Home, NotebookPen, Plus, Target, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { AppIcon } from "@/components/ui/SurfaceCard";

export type AppTab = "home" | "goals" | "debts" | "notes" | "more";

interface TabAction {
  label: string;
  icon: typeof Home;
  onClick: () => void;
}

interface AppTabBarProps {
  tab: AppTab;
  onTabChange: (tab: AppTab) => void;
  actions: TabAction[];
}

const tabs: { id: Exclude<AppTab, "more">; label: string; icon: typeof Home }[] = [
  { id: "home", label: "Главная", icon: Home },
  { id: "goals", label: "Цели", icon: Target },
  { id: "debts", label: "Долги", icon: HandCoins },
  { id: "notes", label: "Заметки", icon: NotebookPen },
];

export function AppTabBar({ tab, onTabChange, actions }: AppTabBarProps) {
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

  const left = tabs.slice(0, 2);
  const right = tabs.slice(2);

  return (
    <div
      ref={rootRef}
      className="fixed inset-x-0 bottom-0 z-40 px-4 pb-[max(1rem,env(safe-area-inset-bottom))] min-[600px]:hidden"
    >
      <div className="mx-auto w-full max-w-md">
        {open ? (
          <div className="mb-3 flex flex-col items-center gap-2">
            {actions.map((action) => (
              <button
                key={action.label}
                type="button"
                onClick={() => {
                  action.onClick();
                  setOpen(false);
                }}
                className="inline-flex min-h-12 items-center gap-2 rounded-full border border-outline-variant bg-surface-container-lowest px-4 py-2 text-sm font-medium text-on-surface shadow-[0_10px_24px_rgb(15_110_92_/_0.12)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
              >
                <AppIcon icon={action.icon} size={16} className="text-primary" />
                {action.label}
              </button>
            ))}
          </div>
        ) : null}

        <nav
          className="relative flex items-center justify-between rounded-full bg-surface-container-lowest px-2 py-2 shadow-[0_12px_32px_rgb(15_110_92_/_0.14)]"
          aria-label="Разделы"
        >
          {left.map((item) => (
            <TabButton key={item.id} item={item} selected={tab === item.id} onSelect={onTabChange} />
          ))}
          <div className="w-16" aria-hidden />
          {right.map((item) => (
            <TabButton key={item.id} item={item} selected={tab === item.id} onSelect={onTabChange} />
          ))}
          <button
            type="button"
            onClick={() => setOpen((value) => !value)}
            aria-expanded={open}
            aria-label={open ? "Закрыть меню" : "Добавить запись"}
            className="absolute left-1/2 top-0 flex size-14 -translate-x-1/2 -translate-y-1/3 items-center justify-center rounded-full bg-primary text-on-primary shadow-[0_10px_24px_rgb(15_110_92_/_0.35)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
          >
            <AppIcon icon={open ? X : Plus} size={22} className="text-on-primary" />
          </button>
        </nav>
      </div>
    </div>
  );
}

export function AppSideRail({
  tab,
  onTabChange,
  actions = [],
}: {
  tab: AppTab;
  onTabChange: (tab: AppTab) => void;
  actions?: TabAction[];
}) {
  return (
    <nav
      className="sticky top-0 hidden h-dvh w-56 shrink-0 flex-col gap-2 border-r border-outline-variant bg-surface-container-low px-3 py-6 min-[600px]:flex"
      aria-label="Разделы"
    >
      <p className="px-3 pb-2 text-sm font-medium text-on-surface-variant">Финансовый контроль</p>
      {tabs.map((item) => {
        const selected = tab === item.id;
        return (
          <button
            key={item.id}
            type="button"
            onClick={() => onTabChange(item.id)}
            aria-current={selected ? "page" : undefined}
            className={`flex min-h-12 items-center gap-3 rounded-full px-4 text-sm font-medium focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary ${
              selected ? "bg-primary-container text-on-primary-container" : "text-on-surface-variant"
            }`}
          >
            <AppIcon icon={item.icon} size={18} className={selected ? "text-on-primary-container" : "text-on-surface-variant"} />
          {item.label}
        </button>
        );
      })}
      {actions.length > 0 ? (
        <div className="mt-4 space-y-2 border-t border-outline-variant pt-4">
          {actions.map((action) => (
            <button
              key={action.label}
              type="button"
              onClick={action.onClick}
              className="flex min-h-12 w-full items-center gap-3 rounded-full px-4 text-sm font-medium text-on-surface focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
            >
              <AppIcon icon={action.icon} size={18} className="text-primary" />
              {action.label}
            </button>
          ))}
        </div>
      ) : null}
    </nav>
  );
}

function TabButton({
  item,
  selected,
  onSelect,
}: {
  item: (typeof tabs)[number];
  selected: boolean;
  onSelect: (tab: AppTab) => void;
}) {
  return (
    <button
      type="button"
      onClick={() => onSelect(item.id)}
      aria-current={selected ? "page" : undefined}
      className={`flex min-h-12 min-w-12 flex-1 flex-col items-center justify-center gap-0.5 rounded-full text-xs font-medium focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary ${
        selected ? "text-primary" : "text-on-surface-variant"
      }`}
    >
      <span
        className={`flex size-8 items-center justify-center rounded-full ${
          selected ? "bg-primary-container" : ""
        }`}
      >
        <AppIcon icon={item.icon} size={16} className={selected ? "text-primary" : "text-on-surface-variant"} />
      </span>
      {item.label}
    </button>
  );
}

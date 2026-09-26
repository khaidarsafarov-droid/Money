import { ArrowLeftRight, Home, Plus, Sparkles, Target, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { AppIcon } from "@/components/ui/SurfaceCard";

export type AppTab = "home" | "activity" | "records" | "advisor" | "more";

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
  { id: "activity", label: "Операции", icon: ArrowLeftRight },
  { id: "records", label: "Записи", icon: Target },
  { id: "advisor", label: "Советник", icon: Sparkles },
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
    <div ref={rootRef} className="fixed inset-x-0 bottom-0 z-40 px-4 pb-4">
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
                className="inline-flex items-center gap-2 rounded-full border border-white/80 bg-white px-4 py-2 text-sm font-medium text-milk-text shadow-[0_10px_24px_rgb(15_110_92_/_0.12)]"
              >
                <AppIcon icon={action.icon} size={16} className="text-milk-accent" />
                {action.label}
              </button>
            ))}
          </div>
        ) : null}

        <nav
          className="relative flex items-center justify-between rounded-full bg-white px-2 py-2 shadow-[0_12px_32px_rgb(15_110_92_/_0.14)]"
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
            className="absolute left-1/2 top-0 flex size-14 -translate-x-1/2 -translate-y-1/3 items-center justify-center rounded-full bg-milk-accent text-white shadow-[0_10px_24px_rgb(15_110_92_/_0.35)]"
          >
            <AppIcon icon={open ? X : Plus} size={22} className="text-white" />
          </button>
        </nav>
      </div>
    </div>
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
      className={`flex min-h-11 min-w-11 flex-1 flex-col items-center justify-center gap-0.5 rounded-full text-[11px] font-medium ${
        selected ? "text-milk-accent" : "text-milk-muted"
      }`}
    >
      <span
        className={`flex size-8 items-center justify-center rounded-full ${
          selected ? "bg-pastel-mint" : ""
        }`}
      >
        <AppIcon icon={item.icon} size={16} className={selected ? "text-milk-accent" : "text-milk-muted"} />
      </span>
      {item.label}
    </button>
  );
}

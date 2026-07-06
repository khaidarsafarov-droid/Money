import { AnimatePresence, motion } from "framer-motion";
import { CheckCircle2, Info, X } from "lucide-react";
import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import { AppIcon } from "@/components/ui/SurfaceCard";

type ToastTone = "success" | "info";

interface ToastMessage {
  id: string;
  text: string;
  tone: ToastTone;
}

interface ToastContextValue {
  showToast: (text: string, tone?: ToastTone) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const showToast = useCallback((text: string, tone: ToastTone = "success") => {
    const id = crypto.randomUUID();
    setToasts((current) => [...current, { id, text, tone }]);
    window.setTimeout(() => {
      setToasts((current) => current.filter((toast) => toast.id !== id));
    }, 3200);
  }, []);

  const value = useMemo(() => ({ showToast }), [showToast]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div
        className="pointer-events-none fixed bottom-28 left-1/2 z-[60] flex w-full max-w-sm -translate-x-1/2 flex-col gap-2 px-4 sm:bottom-8"
        aria-live="polite"
      >
        <AnimatePresence>
          {toasts.map((toast) => (
            <motion.div
              key={toast.id}
              initial={{ opacity: 0, y: 16, scale: 0.96 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 8, scale: 0.96 }}
              className="pointer-events-auto flex items-center gap-3 rounded-2xl border border-white/50 bg-milk-text px-4 py-3 text-sm text-milk-bg shadow-[0_12px_40px_rgb(45_42_38_/_0.2)]"
            >
              <AppIcon
                icon={toast.tone === "success" ? CheckCircle2 : Info}
                size={18}
                className="shrink-0 text-milk-surface"
              />
              <span className="flex-1">{toast.text}</span>
              <button
                type="button"
                className="rounded-full p-1 text-milk-surface/80 hover:text-milk-bg"
                onClick={() => setToasts((current) => current.filter((entry) => entry.id !== toast.id))}
                aria-label="Закрыть"
              >
                <AppIcon icon={X} size={14} />
              </button>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast must be used within ToastProvider");
  }
  return context;
}

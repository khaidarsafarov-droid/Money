import { AnimatePresence, motion } from "framer-motion";
import { X } from "lucide-react";
import { useEffect, type ReactNode } from "react";
import { AppIcon } from "@/components/ui/SurfaceCard";

interface ModalProps {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
}

export function Modal({ open, title, onClose, children, footer }: ModalProps) {
  useEffect(() => {
    if (!open) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [open, onClose]);

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-50 flex items-end justify-center p-4 sm:items-center"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <button
            type="button"
            aria-label="Закрыть"
            className="absolute inset-0 bg-milk-text/20 backdrop-blur-sm"
            onClick={onClose}
          />
          <motion.div
            role="dialog"
            aria-modal="true"
            aria-labelledby="modal-title"
            className="relative z-10 w-full max-w-lg rounded-3xl border border-white/50 bg-milk-bg p-6 shadow-[0_24px_64px_rgb(45_42_38_/_0.12)]"
            initial={{ opacity: 0, y: 24, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 16, scale: 0.98 }}
            transition={{ type: "spring", stiffness: 320, damping: 28 }}
          >
            <div className="mb-6 flex items-start justify-between gap-4">
              <h2 id="modal-title" className="text-xl font-semibold tracking-tight text-milk-text">
                {title}
              </h2>
              <button
                type="button"
                aria-label="Закрыть"
                className="rounded-full p-2 text-milk-muted transition hover:bg-milk-surface/80"
                onClick={onClose}
              >
                <AppIcon icon={X} size={18} />
              </button>
            </div>

            <div className="space-y-4">{children}</div>

            {footer && <div className="mt-6 flex justify-end gap-3">{footer}</div>}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

interface FormFieldProps {
  label: string;
  children: ReactNode;
  error?: string | null;
}

export function FormField({ label, children, error }: FormFieldProps) {
  return (
    <label className="block space-y-2">
      <span className="text-sm font-medium text-milk-muted">{label}</span>
      {children}
      {error && <span className="block text-sm text-milk-error">{error}</span>}
    </label>
  );
}

export const inputClassName =
  "w-full rounded-2xl border border-milk-surface bg-white/60 px-4 py-3 text-sm text-milk-text outline-none transition focus:border-milk-accent focus:ring-2 focus:ring-milk-accent/20";

export function ModalButton({
  children,
  variant = "primary",
  className = "",
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "danger";
}) {
  const base =
    variant === "primary"
      ? "pill-btn-primary"
      : variant === "danger"
        ? "pill-btn bg-milk-error text-white hover:opacity-90"
        : "pill-btn-secondary";

  return (
    <button type="button" className={`${base} ${className}`} {...props}>
      {children}
    </button>
  );
}

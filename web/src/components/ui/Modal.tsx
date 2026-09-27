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
          transition={{ duration: 0.15 }}
        >
          <button
            type="button"
            aria-label="Закрыть"
            className="absolute inset-0 bg-milk-text/40 backdrop-blur-[1px]"
            onClick={onClose}
          />
          <motion.div
            role="dialog"
            aria-modal="true"
            aria-labelledby="modal-title"
            className="relative z-10 w-full max-w-lg rounded-[28px] border border-white/80 bg-white p-5 shadow-[0_18px_40px_rgb(15_110_92_/_0.16)]"
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 8 }}
            transition={{ duration: 0.15 }}
          >
            <div className="mb-4 flex items-start justify-between gap-4">
              <h2 id="modal-title" className="text-lg font-medium text-milk-text">
                {title}
              </h2>
              <button
                type="button"
                aria-label="Закрыть"
                className="rounded-md p-1.5 text-milk-muted transition hover:bg-milk-bg"
                onClick={onClose}
              >
                <AppIcon icon={X} size={16} />
              </button>
            </div>

            <div className="space-y-3">{children}</div>

            {footer && <div className="mt-4 flex justify-end gap-2">{footer}</div>}
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
    <label className="block space-y-1.5">
      <span className="text-sm text-milk-muted">{label}</span>
      {children}
      {error && <span className="block text-sm text-milk-error">{error}</span>}
    </label>
  );
}

export const inputClassName =
  "w-full rounded-2xl border border-milk-surface bg-milk-bg px-3 py-2.5 text-sm text-milk-text outline-none transition focus:border-milk-accent";

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

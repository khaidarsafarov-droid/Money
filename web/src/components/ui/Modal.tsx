import { AnimatePresence, motion } from "framer-motion";
import { X } from "lucide-react";
import { useEffect, useId, useRef, useState, type ReactNode } from "react";
import { AppIcon } from "@/components/ui/SurfaceCard";

interface ModalProps {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
}

const FOCUSABLE = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';

function usePrefersReducedMotion() {
  const [reduced, setReduced] = useState(false);
  useEffect(() => {
    const media = window.matchMedia("(prefers-reduced-motion: reduce)");
    const update = () => setReduced(media.matches);
    update();
    media.addEventListener("change", update);
    return () => media.removeEventListener("change", update);
  }, []);
  return reduced;
}

export function Modal({ open, title, onClose, children, footer }: ModalProps) {
  const dialogRef = useRef<HTMLDivElement>(null);
  const titleId = useId();
  const previouslyFocused = useRef<HTMLElement | null>(null);
  const reduceMotion = usePrefersReducedMotion();

  useEffect(() => {
    if (!open) return;
    previouslyFocused.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const dialog = dialogRef.current;

    const focusable = () =>
      dialog
        ? Array.from(dialog.querySelectorAll<HTMLElement>(FOCUSABLE)).filter(
            (element) => !element.hasAttribute("disabled") && element.tabIndex !== -1,
          )
        : [];

    const field = dialog?.querySelector<HTMLElement>("input, select, textarea");
    (field ?? dialog)?.focus();

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        onClose();
        return;
      }
      if (event.key !== "Tab" || !dialog) return;
      const items = focusable();
      if (items.length === 0) {
        event.preventDefault();
        dialog.focus();
        return;
      }
      const first = items[0];
      const last = items[items.length - 1];
      const active = document.activeElement;
      if (event.shiftKey && (active === first || !dialog.contains(active))) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && (active === last || !dialog.contains(active))) {
        event.preventDefault();
        first.focus();
      }
    };

    window.addEventListener("keydown", onKeyDown);
    return () => {
      window.removeEventListener("keydown", onKeyDown);
      previouslyFocused.current?.focus();
    };
  }, [open, onClose]);

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-50 flex items-end justify-center overscroll-contain p-4 sm:items-center"
          initial={reduceMotion ? false : { opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: reduceMotion ? 0 : 0.15 }}
        >
          <button
            type="button"
            aria-label="Закрыть окно"
            tabIndex={-1}
            className="absolute inset-0 bg-on-surface/40 backdrop-blur-[1px]"
            onClick={onClose}
          />
          <motion.div
            ref={dialogRef}
            role="dialog"
            aria-modal="true"
            aria-labelledby={titleId}
            tabIndex={-1}
            className="relative z-10 max-h-[min(640px,calc(100dvh-2rem))] w-full max-w-lg overflow-y-auto overscroll-contain rounded-[28px] border border-outline-variant bg-surface-container-lowest p-5 shadow-[0_18px_40px_rgb(15_110_92_/_0.16)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
            initial={reduceMotion ? false : { opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={reduceMotion ? { opacity: 0 } : { opacity: 0, y: 8 }}
            transition={{ duration: reduceMotion ? 0 : 0.15 }}
          >
            <div className="mb-4 flex items-start justify-between gap-4">
              <h2 id={titleId} className="text-lg font-medium text-on-surface">
                {title}
              </h2>
              <button
                type="button"
                aria-label="Закрыть"
                className="flex size-12 shrink-0 items-center justify-center rounded-full text-on-surface-variant transition hover:bg-background focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                onClick={onClose}
              >
                <AppIcon icon={X} size={20} />
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
      <span className="text-sm text-on-surface-variant">{label}</span>
      {children}
      {error ? (
        <span className="block text-sm text-error" role="alert">
          {error}
        </span>
      ) : null}
    </label>
  );
}

export const inputClassName = "field-control w-full";

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
        ? "pill-btn bg-error text-on-error hover:opacity-90"
        : "pill-btn-secondary";

  return (
    <button type="button" className={`${base} ${className}`} {...props}>
      {children}
    </button>
  );
}

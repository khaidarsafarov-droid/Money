import { useEffect, useMemo, useState } from "react";
import { remainingAmount } from "@/lib/financeEngine";
import { formatMoney, fromCents } from "@/lib/money";
import type { FinancialItem } from "@/types/finance";
import { FormField, Modal, ModalButton, inputClassName } from "@/components/ui/Modal";

interface PaymentModalProps {
  item: FinancialItem | null;
  open: boolean;
  initialAmountCents?: number;
  onClose: () => void;
  onSubmit: (id: string, amount: string) => { ok: boolean; error?: string };
}

export function PaymentModal({
  item,
  open,
  initialAmountCents = 0,
  onClose,
  onSubmit,
}: PaymentModalProps) {
  const [amount, setAmount] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      setAmount("");
      setError(null);
      return;
    }
    if (initialAmountCents > 0) {
      setAmount(fromCents(initialAmountCents).toFixed(2));
    }
  }, [open, initialAmountCents]);

  const remaining = item ? remainingAmount(item) : 0;
  const isDebt = item?.type === "DEBT";

  const quickAmounts = useMemo(() => {
    if (!item || remaining <= 0) return [];
    const half = Math.floor(remaining / 2);
    const options = [
      { label: "25%", cents: Math.floor(remaining * 0.25) },
      { label: "50%", cents: half > 0 ? half : Math.floor(remaining * 0.5) },
      { label: "Всё", cents: remaining },
    ];
    return options.filter((option) => option.cents > 0);
  }, [item, remaining]);

  if (!item) return null;

  function handleSubmit() {
    const result = onSubmit(item!.id, amount);
    if (!result.ok) {
      setError(result.error ?? "Ошибка");
      return;
    }
    setAmount("");
    setError(null);
    onClose();
  }

  function applyQuickAmount(cents: number) {
    setAmount(fromCents(cents).toFixed(2));
    setError(null);
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={isDebt ? `Оплата — ${item.name}` : `Накопление — ${item.name}`}
      footer={
        <>
          <ModalButton variant="secondary" onClick={onClose}>
            Отмена
          </ModalButton>
          <ModalButton onClick={handleSubmit}>Внести</ModalButton>
        </>
      }
    >
      <p className="text-sm text-on-surface-variant">
        Осталось: <span className="font-semibold text-on-surface">{formatMoney(remaining)}</span>
      </p>

      {quickAmounts.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {quickAmounts.map((option) => (
            <button
              key={option.label}
              type="button"
              onClick={() => applyQuickAmount(option.cents)}
              className="pill-btn-ghost border border-outline-variant/80 px-3 py-1.5 text-xs"
            >
              {option.label} · {formatMoney(option.cents)}
            </button>
          ))}
        </div>
      )}

      <FormField label="Сумма (USD)" error={error}>
        <input
          className={inputClassName}
          name="amount"
          autoComplete="off"
          inputMode="decimal"
          placeholder="50.00"
          value={amount}
          autoFocus
          onChange={(e) => {
            setAmount(e.target.value);
            setError(null);
          }}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              handleSubmit();
            }
          }}
        />
      </FormField>
      <p className="text-xs text-on-surface-variant">
        Частичный платёж переведёт запись в «Частично». Полное закрытие — в «Завершённые».
      </p>
    </Modal>
  );
}

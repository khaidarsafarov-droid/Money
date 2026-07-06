import { useEffect, useState } from "react";
import { FormField, Modal, ModalButton, inputClassName } from "@/components/ui/Modal";

interface AmountModalProps {
  open: boolean;
  title: string;
  hint?: string;
  submitLabel: string;
  onClose: () => void;
  onSubmit: (amount: string) => { ok: boolean; error?: string };
}

export function AmountModal({
  open,
  title,
  hint,
  submitLabel,
  onClose,
  onSubmit,
}: AmountModalProps) {
  const [amount, setAmount] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      setAmount("");
      setError(null);
    }
  }, [open]);

  function handleSubmit() {
    const result = onSubmit(amount);
    if (!result.ok) {
      setError(result.error ?? "Ошибка");
      return;
    }
    setAmount("");
    setError(null);
    onClose();
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      footer={
        <>
          <ModalButton variant="secondary" onClick={onClose}>
            Отмена
          </ModalButton>
          <ModalButton onClick={handleSubmit}>{submitLabel}</ModalButton>
        </>
      }
    >
      {hint ? <p className="text-sm text-milk-muted">{hint}</p> : null}
      <FormField label="Сумма (USD)" error={error}>
        <input
          className={inputClassName}
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
    </Modal>
  );
}

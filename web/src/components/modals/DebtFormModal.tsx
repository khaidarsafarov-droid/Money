import { useState } from "react";
import { FormField, Modal, ModalButton, inputClassName } from "@/components/ui/Modal";
import { parseMoneyInput } from "@/lib/money";

interface DebtFormModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (input: {
    name: string;
    target: number;
    dateGiven: string;
    dateDue: string;
  }) => void;
}

function defaultDueDate(daysFromNow: number): string {
  const date = new Date();
  date.setDate(date.getDate() + daysFromNow);
  return date.toISOString().slice(0, 10);
}

export function DebtFormModal({ open, onClose, onSubmit }: DebtFormModalProps) {
  const [name, setName] = useState("");
  const [target, setTarget] = useState("");
  const [dateGiven, setDateGiven] = useState(() => new Date().toISOString().slice(0, 10));
  const [dateDue, setDateDue] = useState(() => defaultDueDate(30));
  const [error, setError] = useState<string | null>(null);

  function handleSubmit() {
    if (!name.trim()) {
      setError("Укажите имя должника");
      return;
    }
    const targetCents = parseMoneyInput(target);
    if (!targetCents) {
      setError("Введите корректную сумму долга");
      return;
    }
    if (dateDue < dateGiven) {
      setError("Срок возврата не может быть раньше даты выдачи");
      return;
    }
    onSubmit({
      name: name.trim(),
      target: targetCents,
      dateGiven,
      dateDue,
    });
    setName("");
    setTarget("");
    setDateGiven(new Date().toISOString().slice(0, 10));
    setDateDue(defaultDueDate(30));
    setError(null);
    onClose();
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Новый долг"
      footer={
        <>
          <ModalButton variant="secondary" onClick={onClose}>
            Отмена
          </ModalButton>
          <ModalButton onClick={handleSubmit}>Добавить</ModalButton>
        </>
      }
    >
      <FormField label="Кому должен?" error={error}>
        <input
          className={inputClassName}
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Имя или компания"
        />
      </FormField>
      <FormField label="Сумма долга (USD)">
        <input
          className={inputClassName}
          inputMode="decimal"
          value={target}
          onChange={(e) => setTarget(e.target.value)}
          placeholder="500"
        />
      </FormField>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Дата выдачи">
          <input
            className={inputClassName}
            type="date"
            value={dateGiven}
            onChange={(e) => setDateGiven(e.target.value)}
          />
        </FormField>
        <FormField label="Срок возврата">
          <input
            className={inputClassName}
            type="date"
            value={dateDue}
            onChange={(e) => setDateDue(e.target.value)}
          />
        </FormField>
      </div>
    </Modal>
  );
}

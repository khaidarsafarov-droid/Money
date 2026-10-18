import { useState } from "react";
import { FormField, Modal, ModalButton, inputClassName } from "@/components/ui/Modal";
import { parseMoneyInput } from "@/lib/money";

interface GoalFormModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (input: { name: string; target: number; dateDeadline?: string }) => void;
}

function defaultDeadlineIso(): string {
  const date = new Date();
  date.setMonth(date.getMonth() + 3);
  return date.toISOString().slice(0, 10);
}

export function GoalFormModal({ open, onClose, onSubmit }: GoalFormModalProps) {
  const [name, setName] = useState("");
  const [target, setTarget] = useState("");
  const [hasDeadline, setHasDeadline] = useState(false);
  const [deadline, setDeadline] = useState(defaultDeadlineIso);
  const [error, setError] = useState<string | null>(null);

  function handleSubmit() {
    if (!name.trim()) {
      setError("Укажите, для чего эта цель");
      return;
    }
    const targetCents = parseMoneyInput(target);
    if (!targetCents) {
      setError("Введите корректную сумму цели");
      return;
    }
    onSubmit({
      name: name.trim(),
      target: targetCents,
      dateDeadline: hasDeadline ? deadline : undefined,
    });
    setName("");
    setTarget("");
    setHasDeadline(false);
    setDeadline(defaultDeadlineIso());
    setError(null);
    onClose();
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Новая цель"
      footer={
        <>
          <ModalButton variant="secondary" onClick={onClose}>
            Отмена
          </ModalButton>
          <ModalButton onClick={handleSubmit}>Добавить</ModalButton>
        </>
      }
    >
      <FormField
        label="Для чего цель?"
        error={error && !name.trim() ? error : null}
      >
        <input
          className={inputClassName}
          name="goal-name"
          autoComplete="off"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Ремонт грузовика, отпуск, резерв на рейс"
        />
      </FormField>
      <FormField label="Сумма цели (USD)" error={error && name.trim() ? error : null}>
        <input
          className={inputClassName}
          name="goal-amount"
          autoComplete="off"
          inputMode="decimal"
          value={target}
          onChange={(e) => setTarget(e.target.value)}
          placeholder="1000"
        />
      </FormField>
      <p className="text-xs text-on-surface-variant">
        Накопления начинаются с нуля. Сумма записывается только в эту цель.
      </p>
      <FormField label="Срок цели">
        <label className="flex items-center gap-3 text-sm text-on-surface-variant">
          <input
            type="checkbox"
            checked={hasDeadline}
            onChange={(e) => setHasDeadline(e.target.checked)}
            className="size-4 rounded border-outline-variant"
          />
          Рассчитать ежедневный взнос до дедлайна
        </label>
      </FormField>
      {hasDeadline && (
        <FormField label="Дата окончания">
          <input
            type="date"
            name="goal-deadline"
            autoComplete="off"
            className={inputClassName}
            value={deadline}
            onChange={(e) => setDeadline(e.target.value)}
          />
        </FormField>
      )}
    </Modal>
  );
}

import { useState } from "react";
import type { AdvisorMessage, AdvisorPromptId } from "@money/shared";
import { Modal } from "@/components/ui/Modal";

interface DebtAdvisorSheetProps {
  open: boolean;
  messages: AdvisorMessage[];
  onPrompt: (promptId: AdvisorPromptId) => void;
  onClose: () => void;
}

const PROMPTS: { id: AdvisorPromptId; label: string }[] = [
  { id: "which_debt_first", label: "С чего начать?" },
  { id: "what_if_extra", label: "Что если +$50?" },
  { id: "motivation", label: "Мотивация" },
];

export function DebtAdvisorSheet({ open, messages, onPrompt, onClose }: DebtAdvisorSheetProps) {
  const [pending, setPending] = useState<AdvisorPromptId | null>(null);

  return (
    <Modal open={open} title="Советник по долгам" onClose={onClose}>
      <div className="space-y-4">
        <p className="text-sm text-milk-muted">
          Подсказки на основе ваших данных — без интернета и без осуждения.
        </p>

        <div className="flex flex-wrap gap-2">
          {PROMPTS.map((prompt) => (
            <button
              key={prompt.id}
              type="button"
              disabled={pending === prompt.id}
              onClick={() => {
                setPending(prompt.id);
                onPrompt(prompt.id);
                setPending(null);
              }}
              className="pill-btn-secondary text-sm"
            >
              {prompt.label}
            </button>
          ))}
        </div>

        <div className="max-h-72 space-y-2 overflow-y-auto">
          {messages.map((message, index) => (
            <div
              key={`${message.atMs}-${index}`}
              className={`rounded-2xl px-3 py-2 text-sm ${
                message.role === "advisor"
                  ? "bg-milk-surface/80 text-milk-text"
                  : "bg-milk-bg text-milk-muted"
              }`}
            >
              {message.text}
            </div>
          ))}
        </div>
      </div>
    </Modal>
  );
}

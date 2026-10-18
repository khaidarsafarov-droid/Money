import { useState } from "react";
import type { AdvisorMessage, AdvisorPromptId } from "@money/shared";
import { FinOrb } from "@/components/ui/FinOrb";
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

export function AdvisorConversation({
  messages,
  onPrompt,
}: {
  messages: AdvisorMessage[];
  onPrompt: (promptId: AdvisorPromptId) => void;
}) {
  const [pending, setPending] = useState<AdvisorPromptId | null>(null);

  return (
    <div className="space-y-4">
      <div className="space-y-3 text-center">
        <FinOrb />
        <div>
          <h2 className="text-lg font-semibold text-on-surface">Советник</h2>
          <p className="mt-1 text-sm text-on-surface-variant">
            Подсказки на основе ваших данных — без интернета и без осуждения.
          </p>
        </div>
      </div>

      <div className="flex flex-wrap justify-center gap-1.5">
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

      <div className="max-h-[28rem] space-y-2 overflow-y-auto">
        {messages.length === 0 ? (
          <p className="rounded-[22px] bg-surface-container-lowest px-4 py-3 text-center text-sm text-on-surface-variant shadow-[0_8px_24px_rgb(15_110_92_/_0.05)]">
            Чем я могу помочь?
          </p>
        ) : null}
        {messages.map((message, index) => (
          <div
            key={`${message.atMs}-${index}`}
            className={`max-w-[90%] rounded-[22px] px-4 py-3 text-sm ${
              message.role === "advisor"
                ? "bg-surface-container-lowest text-on-surface shadow-[0_8px_24px_rgb(15_110_92_/_0.05)]"
                : "ml-auto bg-primary text-on-primary"
            }`}
          >
            {message.text}
          </div>
        ))}
      </div>
    </div>
  );
}

export function DebtAdvisorSheet({ open, messages, onPrompt, onClose }: DebtAdvisorSheetProps) {
  return (
    <Modal open={open} title="Советник по долгам" onClose={onClose}>
      <AdvisorConversation messages={messages} onPrompt={onPrompt} />
    </Modal>
  );
}

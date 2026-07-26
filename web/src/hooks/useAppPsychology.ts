import { useCallback, useState } from "react";
import {
  appendAdvisorMessage,
  applyPaymentGamification,
  buildAdvisorReply,
  planMicroPayment,
  type AdvisorPromptId,
  type AutopilotDebtInput,
  type AutopilotSettings,
  type GamificationState,
  type PrivacyPreferences,
} from "@money/shared";
import type { FinancialItem } from "@/types/finance";
import { remainingAmount } from "@/lib/financeEngine";
import {
  loadAdvisorHistory,
  loadAutopilotSettings,
  loadGamificationState,
  loadPrivacyPreferences,
  saveAdvisorHistory,
  saveAutopilotSettings,
  saveGamificationState,
  savePrivacyPreferences,
} from "@/lib/appPsychologyStorage";

function toAdvisorDebts(items: FinancialItem[]) {
  return items
    .filter((item) => item.type === "DEBT" && !item.deleted)
    .map((item) => ({
      id: item.id,
      name: item.name,
      remainingCents: remainingAmount(item),
      targetCents: item.target,
      currentCents: item.amount,
      isCompleted: item.status === "COMPLETED",
      dateDueMs: item.dateDue ? new Date(item.dateDue).getTime() : undefined,
    }));
}

function toAutopilotDebts(items: FinancialItem[]): AutopilotDebtInput[] {
  return toAdvisorDebts(items).map((debt) => ({
    id: debt.id,
    name: debt.name,
    remainingCents: debt.remainingCents,
    targetCents: debt.targetCents,
    isCompleted: debt.isCompleted,
    dateDueMs: debt.dateDueMs,
  }));
}

export function useAppPsychology() {
  const [privacy, setPrivacy] = useState<PrivacyPreferences>(() => loadPrivacyPreferences());
  const [gamification, setGamification] = useState<GamificationState>(() => loadGamificationState());
  const [autopilot, setAutopilot] = useState<AutopilotSettings>(() => loadAutopilotSettings());
  const [advisorHistory, setAdvisorHistory] = useState(() => loadAdvisorHistory());

  const updatePrivacy = useCallback((patch: Partial<PrivacyPreferences>) => {
    setPrivacy((current) => {
      const next = { ...current, ...patch };
      savePrivacyPreferences(next);
      return next;
    });
  }, []);

  const updateAutopilot = useCallback((patch: Partial<AutopilotSettings>) => {
    setAutopilot((current) => {
      const next = { ...current, ...patch };
      saveAutopilotSettings(next);
      return next;
    });
  }, []);

  const askAdvisor = useCallback((promptId: AdvisorPromptId, items: FinancialItem[]) => {
    const debts = toAdvisorDebts(items);
    const promptLabels: Record<AdvisorPromptId, string> = {
      which_debt_first: "Какой долг закрыть первым?",
      what_if_extra: "Что если платить на $50 больше?",
      motivation: "Дай мотивацию",
    };
    const nowMs = Date.now();
    const withUser = appendAdvisorMessage(advisorHistory, "user", promptLabels[promptId], nowMs);
    const reply = buildAdvisorReply(promptId, debts, 5_000, nowMs);
    const next = appendAdvisorMessage(withUser, "advisor", reply, nowMs);
    setAdvisorHistory(next);
    saveAdvisorHistory(next);
  }, [advisorHistory]);

  const recordDebtPayment = useCallback((item: FinancialItem, paymentCents: number) => {
    const nowMs = Date.now();
    const next = applyPaymentGamification(
      gamification,
      {
        paidAtMs: nowMs,
        amountCents: paymentCents,
        beforeDueMs: item.dateDue ? new Date(item.dateDue).getTime() : undefined,
      },
      item.amount + paymentCents,
      item.target,
      item.history.filter((payment) => !payment.deleted).length + 1,
      nowMs,
    );
    setGamification(next);
    saveGamificationState(next);
  }, [gamification]);

  const planAutopilotPayment = useCallback((items: FinancialItem[]) => {
    return planMicroPayment(toAutopilotDebts(items), autopilot);
  }, [autopilot]);

  return {
    privacy,
    updatePrivacy,
    gamification,
    autopilot,
    updateAutopilot,
    advisorHistory,
    askAdvisor,
    recordDebtPayment,
    planAutopilotPayment,
  };
}

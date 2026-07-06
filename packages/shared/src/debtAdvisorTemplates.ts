import {
  calculateFreedomProjection,
  type DebtFreedomDebtInput,
  type FreedomProjection,
} from "./debtFreedomEngine";

export type AdvisorPromptId =
  | "which_debt_first"
  | "what_if_extra"
  | "motivation";

export interface AdvisorDebtInput {
  id: string;
  name: string;
  remainingCents: number;
  targetCents: number;
  currentCents: number;
  isCompleted: boolean;
  dateDueMs?: number;
}

export interface AdvisorMessage {
  role: "user" | "advisor";
  text: string;
  atMs: number;
}

export function pickSnowballDebt(debts: AdvisorDebtInput[]): AdvisorDebtInput | null {
  const active = debts.filter((d) => !d.isCompleted && d.remainingCents > 0);
  if (active.length === 0) return null;
  return [...active].sort((a, b) => a.remainingCents - b.remainingCents)[0] ?? null;
}

export function pickAvalancheDebt(debts: AdvisorDebtInput[]): AdvisorDebtInput | null {
  const active = debts.filter((d) => !d.isCompleted && d.remainingCents > 0);
  if (active.length === 0) return null;
  return [...active].sort((a, b) => {
    const dueA = a.dateDueMs ?? Number.MAX_SAFE_INTEGER;
    const dueB = b.dateDueMs ?? Number.MAX_SAFE_INTEGER;
    if (dueA !== dueB) return dueA - dueB;
    return b.remainingCents - a.remainingCents;
  })[0] ?? null;
}

export function buildAdvisorReply(
  promptId: AdvisorPromptId,
  debts: AdvisorDebtInput[],
  extraMonthlyCents = 5_000,
  nowMs = Date.now(),
): string {
  const active = debts.filter((d) => !d.isCompleted && d.remainingCents > 0);
  if (active.length === 0) {
    return "У вас нет активных долгов — это уже победа. Можно сфокусироваться на целях.";
  }

  const freedomInputs: DebtFreedomDebtInput[] = active.map((d) => ({
    remainingCents: d.remainingCents,
    targetCents: d.targetCents,
    currentCents: d.currentCents,
    isCompleted: d.isCompleted,
    payments: [],
    dateDueMs: d.dateDueMs,
  }));

  switch (promptId) {
    case "which_debt_first": {
      const snowball = pickSnowballDebt(active);
      if (!snowball) return "Сначала добавьте долг в приложение.";
      return `Начните с «${snowball.name}» — это самый маленький остаток. Быстрая победа даст энергию закрыть остальные.`;
    }
    case "what_if_extra": {
      const base = calculateFreedomProjection(freedomInputs, 0, nowMs);
      const boosted = calculateFreedomProjection(freedomInputs, extraMonthlyCents, nowMs);
      return formatExtraPaymentReply(base, boosted, extraMonthlyCents);
    }
    case "motivation": {
      const projection = calculateFreedomProjection(freedomInputs, 0, nowMs);
      const closed = debts.filter((d) => d.isCompleted).length;
      if (!projection.freedomDateMs) {
        return `Вы уже закрыли ${closed} долг(ов). Продолжайте — каждый платёж приближает свободу.`;
      }
      const date = new Date(projection.freedomDateMs).toLocaleDateString("ru-RU", {
        day: "numeric",
        month: "long",
        year: "numeric",
      });
      return `Вы на пути к свободе. При текущем темпе цель — ${date}. Каждый платёж сдвигает эту дату ближе.`;
    }
    default: {
      const _exhaustive: never = promptId;
      return String(_exhaustive);
    }
  }
}

function formatExtraPaymentReply(
  base: FreedomProjection,
  boosted: FreedomProjection,
  extraMonthlyCents: number,
): string {
  const extra = (extraMonthlyCents / 100).toFixed(0);
  if (!base.hasActiveDebts || !boosted.freedomDateMs || !base.freedomDateMs) {
    return `Если добавить $${extra} в месяц, вы быстрее разгрузите бюджет.`;
  }
  const saved = base.monthsSavedWithExtra ?? boosted.monthsSavedWithExtra ?? 0;
  if (saved > 0) {
    return `+$${extra}/мес сократит путь примерно на ${saved} мес. Это ощутимый сдвиг без героических усилий.`;
  }
  return `+$${extra}/мес ускорит погашение. Попробуйте один месяц — и посмотрите, как изменится дата свободы.`;
}

export function appendAdvisorMessage(
  history: AdvisorMessage[],
  role: AdvisorMessage["role"],
  text: string,
  nowMs = Date.now(),
  maxItems = 20,
): AdvisorMessage[] {
  return [...history, { role, text, atMs: nowMs }].slice(-maxItems);
}

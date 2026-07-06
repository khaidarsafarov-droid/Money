export type AutopilotStrategy = "snowball" | "avalanche";

export interface AutopilotDebtInput {
  id: string;
  name: string;
  remainingCents: number;
  targetCents: number;
  isCompleted: boolean;
  dateDueMs?: number;
}

export interface AutopilotSettings {
  enabled: boolean;
  dailyAmountCents: number;
  strategy: AutopilotStrategy;
  reminderHour: number;
  reminderMinute: number;
}

export const DEFAULT_AUTOPILOT: AutopilotSettings = {
  enabled: false,
  dailyAmountCents: 300,
  strategy: "snowball",
  reminderHour: 20,
  reminderMinute: 0,
};

export function pickTargetDebt(
  debts: AutopilotDebtInput[],
  strategy: AutopilotStrategy,
): AutopilotDebtInput | null {
  const active = debts.filter((d) => !d.isCompleted && d.remainingCents > 0);
  if (active.length === 0) return null;

  if (strategy === "snowball") {
    return [...active].sort((a, b) => a.remainingCents - b.remainingCents)[0] ?? null;
  }

  return [...active].sort((a, b) => {
    const dueA = a.dateDueMs ?? Number.MAX_SAFE_INTEGER;
    const dueB = b.dateDueMs ?? Number.MAX_SAFE_INTEGER;
    if (dueA !== dueB) return dueA - dueB;
    return b.remainingCents - a.remainingCents;
  })[0] ?? null;
}

export function microPaymentAmountCents(settings: AutopilotSettings): number {
  return Math.max(100, settings.dailyAmountCents);
}

export interface MicroPaymentPlan {
  debtId: string;
  debtName: string;
  amountCents: number;
}

export function planMicroPayment(
  debts: AutopilotDebtInput[],
  settings: AutopilotSettings,
): MicroPaymentPlan | null {
  if (!settings.enabled) return null;
  const target = pickTargetDebt(debts, settings.strategy);
  if (!target) return null;
  const amountCents = Math.min(microPaymentAmountCents(settings), target.remainingCents);
  if (amountCents <= 0) return null;
  return {
    debtId: target.id,
    debtName: target.name,
    amountCents,
  };
}

export type AutopilotStrategy = "snowball" | "avalanche";

export interface AutopilotDebtInput {
  id: string;
  name: string;
  remainingCents: number;
  targetCents: number;
  isCompleted: boolean;
  dateDueMs?: number;
  interestRateBps?: number;
  loanCategory?: string;
}

export interface AutopilotSettings {
  enabled: boolean;
  dailyAmountCents: number;
  strategy: AutopilotStrategy;
  reminderHour: number;
  reminderMinute: number;
  roundPaymentsToTen: boolean;
}

export const DEFAULT_AUTOPILOT: AutopilotSettings = {
  enabled: false,
  dailyAmountCents: 300,
  strategy: "snowball",
  reminderHour: 20,
  reminderMinute: 0,
  roundPaymentsToTen: false,
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
    const rateA = a.interestRateBps ?? 0;
    const rateB = b.interestRateBps ?? 0;
    if (rateA !== rateB) return rateB - rateA;
    const dueA = a.dateDueMs ?? Number.MAX_SAFE_INTEGER;
    const dueB = b.dateDueMs ?? Number.MAX_SAFE_INTEGER;
    if (dueA !== dueB) return dueA - dueB;
    return b.remainingCents - a.remainingCents;
  })[0] ?? null;
}

export function roundPaymentToTenCents(amountCents: number): number {
  if (amountCents <= 0) return amountCents;
  const dollars = Math.ceil(amountCents / 100);
  const roundedDollars = Math.ceil(dollars / 10) * 10;
  return roundedDollars * 100;
}

export function microPaymentAmountCents(settings: AutopilotSettings): number {
  const base = Math.max(100, settings.dailyAmountCents);
  return settings.roundPaymentsToTen ? roundPaymentToTenCents(base) : base;
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

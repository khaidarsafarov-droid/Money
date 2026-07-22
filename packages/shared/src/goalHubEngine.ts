import { calculateFreedomProjection, type DebtFreedomDebtInput } from "./debtFreedomEngine";
import { pickTargetDebt, type AutopilotDebtInput, type AutopilotStrategy } from "./debtAutopilot";

const MS_PER_DAY = 86_400_000;

export interface DebtPayoffItemInput {
  id: string;
  name: string;
  remainingCents: number;
  targetCents: number;
  currentCents: number;
  isCompleted: boolean;
  interestRateBps?: number;
  dateDueMs?: number;
  payments: { amountCents: number; paidAtMs: number }[];
  plannedMonthlyCents?: number;
}

export interface DebtPayoffPlanItem {
  id: string;
  name: string;
  remainingCents: number;
  projectedCloseDateMs: number | null;
  progressPercent: number;
  hasPaymentData: boolean;
}

function estimateMonthlyFromPayments(
  debt: DebtPayoffItemInput,
  nowMs: number,
): number | null {
  if (debt.plannedMonthlyCents && debt.plannedMonthlyCents > 0) {
    return debt.plannedMonthlyCents;
  }

  const recent = debt.payments.filter((p) => p.paidAtMs >= nowMs - 30 * MS_PER_DAY);
  if (recent.length === 0) return null;

  const total = recent.reduce((sum, p) => sum + p.amountCents, 0);
  const oldest = Math.min(...recent.map((p) => p.paidAtMs));
  const days = Math.max(1, (nowMs - oldest) / MS_PER_DAY);
  return Math.ceil((total / days) * 30);
}

export function projectDebtCloseDate(
  debt: DebtPayoffItemInput,
  nowMs = Date.now(),
): { projectedCloseDateMs: number | null; hasPaymentData: boolean } {
  const monthly = estimateMonthlyFromPayments(debt, nowMs);
  if (!monthly || debt.remainingCents <= 0) {
    return { projectedCloseDateMs: null, hasPaymentData: false };
  }

  const months = Math.ceil(debt.remainingCents / monthly);
  const date = new Date(nowMs);
  date.setMonth(date.getMonth() + months);

  if (debt.dateDueMs && debt.dateDueMs > nowMs) {
    return {
      projectedCloseDateMs: Math.min(debt.dateDueMs, date.getTime()),
      hasPaymentData: true,
    };
  }

  return { projectedCloseDateMs: date.getTime(), hasPaymentData: true };
}

export function buildPayoffPlan(
  debts: DebtPayoffItemInput[],
  strategy: AutopilotStrategy,
  nowMs = Date.now(),
): DebtPayoffPlanItem[] {
  const active = debts.filter((d) => !d.isCompleted && d.remainingCents > 0);

  const sorted = [...active].sort((a, b) => {
    if (strategy === "snowball") {
      return a.remainingCents - b.remainingCents;
    }
    const rateA = a.interestRateBps ?? 0;
    const rateB = b.interestRateBps ?? 0;
    if (rateA !== rateB) return rateB - rateA;
    return b.remainingCents - a.remainingCents;
  });

  return sorted.map((debt) => {
    const { projectedCloseDateMs, hasPaymentData } = projectDebtCloseDate(debt, nowMs);
    const progressPercent =
      debt.targetCents > 0
        ? Math.min(100, Math.floor((debt.currentCents / debt.targetCents) * 100))
        : 0;

    return {
      id: debt.id,
      name: debt.name,
      remainingCents: debt.remainingCents,
      projectedCloseDateMs,
      progressPercent,
      hasPaymentData,
    };
  });
}

export function freedomSummary(
  debts: DebtPayoffItemInput[],
  extraMonthlyCents = 0,
  nowMs = Date.now(),
): {
  freedomDateMs: number | null;
  daysRemaining: number | null;
  totalRemainingCents: number;
  progressPercent: number;
} {
  const freedomInputs: DebtFreedomDebtInput[] = debts
    .filter((d) => !d.isCompleted && d.remainingCents > 0)
    .map((d) => ({
      remainingCents: d.remainingCents,
      targetCents: d.targetCents,
      currentCents: d.currentCents,
      isCompleted: d.isCompleted,
      payments: d.payments,
      dateDueMs: d.dateDueMs,
    }));

  const projection = calculateFreedomProjection(freedomInputs, extraMonthlyCents, nowMs);
  const totalOriginal = debts.reduce((sum, d) => sum + d.targetCents, 0);
  const totalPaid = debts.reduce((sum, d) => sum + d.currentCents, 0);
  const progressPercent =
    totalOriginal > 0 ? Math.min(100, Math.floor((totalPaid / totalOriginal) * 100)) : 0;

  const daysRemaining =
    projection.freedomDateMs != null
      ? Math.max(0, Math.ceil((projection.freedomDateMs - nowMs) / MS_PER_DAY))
      : null;

  return {
    freedomDateMs: projection.freedomDateMs,
    daysRemaining,
    totalRemainingCents: projection.totalRemainingCents,
    progressPercent,
  };
}

export function daysUntilPayday(paydayDayOfMonth: number, nowMs = Date.now()): number {
  const now = new Date(nowMs);
  const year = now.getFullYear();
  const month = now.getMonth();
  let payday = new Date(year, month, paydayDayOfMonth, 12, 0, 0);
  if (payday.getTime() <= nowMs) {
    payday = new Date(year, month + 1, paydayDayOfMonth, 12, 0, 0);
  }
  return Math.max(0, Math.ceil((payday.getTime() - nowMs) / MS_PER_DAY));
}

export function toAutopilotInput(debt: DebtPayoffItemInput): AutopilotDebtInput {
  return {
    id: debt.id,
    name: debt.name,
    remainingCents: debt.remainingCents,
    targetCents: debt.targetCents,
    isCompleted: debt.isCompleted,
    dateDueMs: debt.dateDueMs,
    interestRateBps: debt.interestRateBps,
  };
}

export { pickTargetDebt };

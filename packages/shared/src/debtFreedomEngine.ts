/** Milestone thresholds for debt payoff journey (percent of target). */
export const DEBT_MILESTONE_PERCENTS = [25, 50, 75, 100] as const;

export type DebtMilestonePercent = (typeof DEBT_MILESTONE_PERCENTS)[number];

export interface DebtMilestone {
  percent: DebtMilestonePercent;
  reached: boolean;
  centsToReach: number;
}

export interface DebtMilestonesResult {
  progressPercent: number;
  milestones: DebtMilestone[];
  nextMilestone: DebtMilestone | null;
}

export interface DebtPaymentSample {
  amountCents: number;
  paidAtMs: number;
}

export interface DebtFreedomDebtInput {
  remainingCents: number;
  targetCents: number;
  currentCents: number;
  isCompleted: boolean;
  payments: DebtPaymentSample[];
  dateDueMs?: number;
}

export interface FreedomProjection {
  freedomDateMs: number | null;
  monthsRemaining: number | null;
  totalRemainingCents: number;
  monthlyPaymentCents: number;
  monthsSavedWithExtra: number | null;
  hasActiveDebts: boolean;
}

const MS_PER_DAY = 86_400_000;
const MS_PER_MONTH = 30 * MS_PER_DAY;
const PAYMENT_WINDOW_MS = 90 * MS_PER_DAY;
const SMALL_DEBT_CENTS = 10_000;
const MIN_MONTHLY_CENTS = 100;
const DEFAULT_ACCELERATE_CENTS = 500;

export function computeDebtMilestones(
  currentCents: number,
  targetCents: number,
): DebtMilestonesResult {
  if (targetCents <= 0) {
    return { progressPercent: 0, milestones: [], nextMilestone: null };
  }

  const progressPercent = Math.min(
    100,
    Math.floor((currentCents / targetCents) * 100),
  );

  const milestones: DebtMilestone[] = DEBT_MILESTONE_PERCENTS.map((percent) => {
    const thresholdCents = Math.ceil((targetCents * percent) / 100);
    const reached = currentCents >= thresholdCents;
    const centsToReach = Math.max(thresholdCents - currentCents, 0);
    return { percent, reached, centsToReach };
  });

  const nextMilestone = milestones.find((m) => !m.reached) ?? null;

  return { progressPercent, milestones, nextMilestone };
}

export function suggestAcceleratePaymentCents(
  remainingCents: number,
  targetCents: number,
  currentCents: number,
): number {
  if (remainingCents <= 0) return 0;

  const { nextMilestone } = computeDebtMilestones(currentCents, targetCents);
  const milestoneGap = nextMilestone?.centsToReach ?? remainingCents;

  const suggested = Math.min(
    remainingCents,
    Math.max(DEFAULT_ACCELERATE_CENTS, milestoneGap),
  );

  return suggested;
}

export function estimateMonthlyPaymentCents(
  debts: DebtFreedomDebtInput[],
  nowMs = Date.now(),
): number {
  const active = debts.filter((d) => !d.isCompleted && d.remainingCents > 0);
  if (active.length === 0) return 0;

  const recentPayments = active
    .flatMap((d) => d.payments)
    .filter((p) => p.paidAtMs >= nowMs - PAYMENT_WINDOW_MS);

  if (recentPayments.length > 0) {
    const total = recentPayments.reduce((sum, p) => sum + p.amountCents, 0);
    const oldest = Math.min(...recentPayments.map((p) => p.paidAtMs));
    const months = Math.max(1, (nowMs - oldest) / MS_PER_MONTH);
    return Math.ceil(total / months);
  }

  const totalRemaining = active.reduce((sum, d) => sum + d.remainingCents, 0);
  const withDue = active.filter((d) => d.dateDueMs != null);

  if (withDue.length > 0) {
    const nearestDue = Math.min(...withDue.map((d) => d.dateDueMs!));
    const monthsUntilDue = Math.max(
      1,
      Math.ceil((nearestDue - nowMs) / MS_PER_MONTH),
    );
    return Math.ceil(totalRemaining / monthsUntilDue);
  }

  return Math.max(DEFAULT_ACCELERATE_CENTS, Math.ceil(totalRemaining / 12));
}

export function calculateFreedomProjection(
  debts: DebtFreedomDebtInput[],
  extraMonthlyCents = 0,
  nowMs = Date.now(),
): FreedomProjection {
  const active = debts.filter((d) => !d.isCompleted && d.remainingCents > 0);
  const totalRemaining = active.reduce((sum, d) => sum + d.remainingCents, 0);

  if (active.length === 0) {
    return {
      freedomDateMs: null,
      monthsRemaining: 0,
      totalRemainingCents: 0,
      monthlyPaymentCents: 0,
      monthsSavedWithExtra: null,
      hasActiveDebts: false,
    };
  }

  const baseMonthly = estimateMonthlyPaymentCents(debts, nowMs);
  const effectiveMonthly = Math.max(MIN_MONTHLY_CENTS, baseMonthly + extraMonthlyCents);
  const monthsRemaining = Math.ceil(totalRemaining / effectiveMonthly);

  const freedomDate = new Date(nowMs);
  freedomDate.setMonth(freedomDate.getMonth() + monthsRemaining);

  let monthsSavedWithExtra: number | null = null;
  if (extraMonthlyCents > 0 && baseMonthly > 0) {
    const baseMonths = Math.ceil(totalRemaining / Math.max(MIN_MONTHLY_CENTS, baseMonthly));
    monthsSavedWithExtra = Math.max(0, baseMonths - monthsRemaining);
  }

  return {
    freedomDateMs: freedomDate.getTime(),
    monthsRemaining,
    totalRemainingCents: totalRemaining,
    monthlyPaymentCents: baseMonthly,
    monthsSavedWithExtra,
    hasActiveDebts: true,
  };
}

export function isSmallDebt(remainingCents: number): boolean {
  return remainingCents > 0 && remainingCents <= SMALL_DEBT_CENTS;
}

export function estimatePaymentsToClose(
  remainingCents: number,
  paymentCents: number,
): number {
  if (paymentCents <= 0 || remainingCents <= 0) return 0;
  return Math.ceil(remainingCents / paymentCents);
}

export function addMonthsToDate(fromMs: number, months: number): number {
  const date = new Date(fromMs);
  date.setMonth(date.getMonth() + months);
  return date.getTime();
}

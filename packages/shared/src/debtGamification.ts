import { computeDebtMilestones, DEBT_MILESTONE_PERCENTS } from "./debtFreedomEngine";

export const BADGE_FIRST_STEP = "first_step";
export const BADGE_HALF_WAY = "half_way";
export const BADGE_FREEDOM = "freedom";
export const BADGE_STREAK_3 = "streak_3";

export interface PaymentEvent {
  paidAtMs: number;
  amountCents: number;
  beforeDueMs?: number;
}

export interface GamificationState {
  streakCount: number;
  lastPaymentIsoDate: string | null;
  unlockedBadges: string[];
  lastWinMessage: string | null;
}

export const EMPTY_GAMIFICATION: GamificationState = {
  streakCount: 0,
  lastPaymentIsoDate: null,
  unlockedBadges: [],
  lastWinMessage: null,
};

export function isoDateFromMs(ms: number): string {
  return new Date(ms).toISOString().slice(0, 10);
}

export function daysBetweenIsoDates(fromIso: string, toIso: string): number {
  const from = new Date(`${fromIso}T00:00:00`).getTime();
  const to = new Date(`${toIso}T00:00:00`).getTime();
  return Math.round((to - from) / 86_400_000);
}

export function computeStreakAfterPayment(
  previous: GamificationState,
  payment: PaymentEvent,
  _nowMs = Date.now(),
): number {
  const paymentDay = isoDateFromMs(payment.paidAtMs);

  if (payment.beforeDueMs != null && payment.paidAtMs > payment.beforeDueMs) {
    return 1;
  }

  if (!previous.lastPaymentIsoDate) {
    return 1;
  }

  const gap = daysBetweenIsoDates(previous.lastPaymentIsoDate, paymentDay);
  if (gap === 0) return previous.streakCount;
  if (gap === 1) return previous.streakCount + 1;
  return 1;
}

export function badgesForDebtProgress(
  currentCents: number,
  targetCents: number,
  paymentCount: number,
): string[] {
  const badges: string[] = [];
  if (paymentCount >= 1) badges.push(BADGE_FIRST_STEP);

  const milestones = computeDebtMilestones(currentCents, targetCents);
  if (milestones.milestones.some((m) => m.percent >= 50 && m.reached)) {
    badges.push(BADGE_HALF_WAY);
  }
  if (milestones.milestones.some((m) => m.percent === 100 && m.reached)) {
    badges.push(BADGE_FREEDOM);
  }
  return badges;
}

export function applyPaymentGamification(
  previous: GamificationState,
  payment: PaymentEvent,
  currentCents: number,
  targetCents: number,
  paymentCount: number,
  nowMs = Date.now(),
): GamificationState {
  const streakCount = computeStreakAfterPayment(previous, payment, nowMs);
  const progressBadges = badgesForDebtProgress(currentCents, targetCents, paymentCount);
  const unlocked = new Set([...previous.unlockedBadges, ...progressBadges]);
  if (streakCount >= 3) unlocked.add(BADGE_STREAK_3);

  const milestones = computeDebtMilestones(currentCents, targetCents);
  const next = milestones.nextMilestone;
  let lastWinMessage: string | null = null;
  if (progressBadges.includes(BADGE_FREEDOM)) {
    lastWinMessage = "Долг закрыт — отличная победа!";
  } else if (next) {
    lastWinMessage = `До вехи ${next.percent}% осталось немного. Вы справляетесь.`;
  } else {
    lastWinMessage = `Серия платежей: ${streakCount}. Так держать.`;
  }

  return {
    streakCount,
    lastPaymentIsoDate: isoDateFromMs(payment.paidAtMs),
    unlockedBadges: [...unlocked],
    lastWinMessage,
  };
}

export function badgeLabel(badgeId: string): string {
  switch (badgeId) {
    case BADGE_FIRST_STEP:
      return "Первый шаг";
    case BADGE_HALF_WAY:
      return "Полпути";
    case BADGE_FREEDOM:
      return "Свобода";
    case BADGE_STREAK_3:
      return "Серия ×3";
    default:
      return badgeId;
  }
}

export { DEBT_MILESTONE_PERCENTS };

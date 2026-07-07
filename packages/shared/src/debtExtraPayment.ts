const MS_PER_DAY = 86_400_000;
const MS_PER_MONTH = 30 * MS_PER_DAY;

export interface ExtraPaymentImpactInput {
  remainingCents: number;
  monthlyPaymentCents: number;
  extraPaymentCents: number;
  interestRateBps?: number;
  dateDueMs?: number;
}

export interface ExtraPaymentImpact {
  daysSaved: number;
  interestSavedCents: number;
  newFreedomDateMs: number | null;
}

export function estimateMonthlyPaymentForDebt(
  remainingCents: number,
  dateDueMs: number | undefined,
  nowMs: number,
): number {
  if (remainingCents <= 0) return 0;
  if (dateDueMs != null && dateDueMs > nowMs) {
    const monthsUntilDue = Math.max(1, (dateDueMs - nowMs) / MS_PER_MONTH);
    return Math.ceil(remainingCents / monthsUntilDue);
  }
  return Math.max(500, Math.ceil(remainingCents / 12));
}

export function calculateExtraPaymentImpact(
  input: ExtraPaymentImpactInput,
  nowMs = Date.now(),
): ExtraPaymentImpact {
  const {
    remainingCents,
    monthlyPaymentCents,
    extraPaymentCents,
    interestRateBps = 0,
    dateDueMs,
  } = input;

  if (remainingCents <= 0 || extraPaymentCents <= 0) {
    return { daysSaved: 0, interestSavedCents: 0, newFreedomDateMs: null };
  }

  const baseMonthly = Math.max(100, monthlyPaymentCents);
  const boostedMonthly = baseMonthly + extraPaymentCents;

  const baseMonths = Math.ceil(remainingCents / baseMonthly);
  const boostedMonths = Math.ceil(remainingCents / boostedMonthly);
  const monthsSaved = Math.max(0, baseMonths - boostedMonths);
  const daysSaved = monthsSaved * 30;

  const annualRate = interestRateBps / 10_000;
  const monthlyRate = annualRate / 12;
  const interestSavedCents =
    monthlyRate > 0
      ? Math.round(remainingCents * monthlyRate * monthsSaved)
      : 0;

  const paceFreedom = new Date(nowMs);
  paceFreedom.setMonth(paceFreedom.getMonth() + boostedMonths);
  const newFreedomDateMs =
    dateDueMs != null && dateDueMs > nowMs
      ? Math.min(dateDueMs, paceFreedom.getTime())
      : paceFreedom.getTime();

  return { daysSaved, interestSavedCents, newFreedomDateMs };
}

export function formatExtraPaymentHint(
  impact: ExtraPaymentImpact,
  extraPaymentCents: number,
): string {
  const extra = (extraPaymentCents / 100).toFixed(0);
  if (impact.daysSaved <= 0) {
    return `Добавьте $${extra} сверх платежа — это ускорит закрытие долга.`;
  }
  const saved = (impact.interestSavedCents / 100).toFixed(0);
  if (impact.interestSavedCents > 0) {
    return `+$${extra} сэкономит ~$${saved} на процентах и закроет долг на ${impact.daysSaved} дн. раньше.`;
  }
  return `+$${extra} закроет долг на ${impact.daysSaved} дн. раньше.`;
}

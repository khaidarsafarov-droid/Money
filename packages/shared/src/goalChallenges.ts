export type ChallengeTemplateId =
  | "close_debt_14d"
  | "no_coffee_month"
  | "three_early_payments";

export interface ActiveChallenge {
  id: string;
  templateId: ChallengeTemplateId;
  debtId?: string;
  debtName?: string;
  startedAtMs: number;
  targetDays?: number;
  progressDays: number;
  completed: boolean;
}

export interface ChallengeTemplate {
  id: ChallengeTemplateId;
  title: string;
  description: string;
  requiresDebtPick: boolean;
  targetDays?: number;
}

export const CHALLENGE_TEMPLATES: ChallengeTemplate[] = [
  {
    id: "close_debt_14d",
    title: "Закрыть долг за 14 дней",
    description: "Выберите долг и поставьте срок",
    requiresDebtPick: true,
    targetDays: 14,
  },
  {
    id: "no_coffee_month",
    title: "Сэкономить $50 на кофе за месяц",
    description: "Отмечайте каждый день без лишних трат на кофе",
    requiresDebtPick: false,
    targetDays: 30,
  },
  {
    id: "three_early_payments",
    title: "Внести 3 платежа досрочно",
    description: "Отслеживается автоматически по вашим оплатам",
    requiresDebtPick: false,
  },
];

export const GOAL_BADGE_FIRST_PAYMENT = "first_payment";
export const GOAL_BADGE_SNOWBALL = "snowball";
export const GOAL_BADGE_HERO = "hero";
export const GOAL_BADGE_FREEDOM = "freedom";

export function goalBadgeLabelRu(badgeId: string): string {
  switch (badgeId) {
    case GOAL_BADGE_FIRST_PAYMENT:
      return "Первый платёж";
    case GOAL_BADGE_SNOWBALL:
      return "Снежный ком";
    case GOAL_BADGE_HERO:
      return "Герой";
    case GOAL_BADGE_FREEDOM:
      return "Долги закрыты";
    default:
      return badgeId;
  }
}

export function computeGoalBadges(input: {
  paymentCount: number;
  smallestDebtClosed: boolean;
  totalDebtProgressPercent: number;
  allDebtsClosed: boolean;
}): string[] {
  const badges: string[] = [];
  if (input.paymentCount >= 1) badges.push(GOAL_BADGE_FIRST_PAYMENT);
  if (input.smallestDebtClosed) badges.push(GOAL_BADGE_SNOWBALL);
  if (input.totalDebtProgressPercent >= 50) badges.push(GOAL_BADGE_HERO);
  if (input.allDebtsClosed) badges.push(GOAL_BADGE_FREEDOM);
  return badges;
}

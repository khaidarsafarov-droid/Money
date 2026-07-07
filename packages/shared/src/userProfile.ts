import type { Psychotype } from "./psychotype";
import type { UserFearId } from "./userFears";

export type UserGender = "MALE" | "FEMALE" | "UNSPECIFIED";

export type UserEmotion = "HAPPY" | "NEUTRAL" | "SAD" | "ANGRY" | "THINKING";

export interface UserProfile {
  displayName: string;
  gender: UserGender;
  age: number | null;
  fears: UserFearId[];
  customFear: string;
  goalText: string;
  emotion: UserEmotion | null;
  priority: string;
  psychotype: Psychotype | null;
  onboardingCompleted: boolean;
}

export const DEFAULT_USER_PROFILE: UserProfile = {
  displayName: "",
  gender: "UNSPECIFIED",
  age: null,
  fears: [],
  customFear: "",
  goalText: "",
  emotion: null,
  priority: "",
  psychotype: null,
  onboardingCompleted: false,
};

export const DEFAULT_DISPLAY_NAME = "Друг";
export const DEFAULT_GOAL_TEXT = "Стать финансово свободным";

const NAME_PATTERN = /^[\p{L}][\p{L}\s'-]{1,}$/u;

export function isValidDisplayName(name: string): boolean {
  const trimmed = name.trim();
  return trimmed.length >= 2 && NAME_PATTERN.test(trimmed);
}

export function isValidAge(age: number | null | undefined): boolean {
  if (age == null) return true;
  return Number.isInteger(age) && age >= 18 && age <= 99;
}

export function resolveDisplayName(name: string): string {
  const trimmed = name.trim();
  return trimmed.length >= 2 ? trimmed : DEFAULT_DISPLAY_NAME;
}

export function resolveGoalText(goal: string): string {
  const trimmed = goal.trim();
  return trimmed.length > 0 ? trimmed : DEFAULT_GOAL_TEXT;
}

export function personalizedGreeting(name: string, progressPercent: number): string {
  const who = resolveDisplayName(name);
  if (progressPercent > 0) {
    return `Привет, ${who}! Сегодня вы на ${progressPercent}% ближе к свободе.`;
  }
  return `Привет, ${who}! Давайте сделаем первый шаг к свободе.`;
}

export function psychotypeOnboardingDescription(type: Psychotype): string {
  switch (type) {
    case "SPENDER":
      return "Вы любите тратить — это даёт вам энергию. Давайте направим её в нужное русло.";
    case "SAVER":
      return "Вы умеете копить — это ваша суперсила. Научимся тратить с удовольствием.";
    case "INDIFFERENT":
      return "Деньги не ваша страсть, но контроль над ними даст вам свободу.";
    case "PLAYER":
      return "Риск — ваше топливо. Используем его для роста, а не для потерь.";
    case "BALANCED":
      return "Вы на верном пути — сохраняйте баланс.";
    default: {
      const _exhaustive: never = type;
      return String(_exhaustive);
    }
  }
}

export function emotionEmoji(emotion: UserEmotion | null): string {
  switch (emotion) {
    case "HAPPY":
      return "😊";
    case "NEUTRAL":
      return "😐";
    case "SAD":
      return "😔";
    case "ANGRY":
      return "😡";
    case "THINKING":
      return "🤔";
    default:
      return "🎯";
  }
}

export function affirmationWithName(name: string, baseAffirmation: string, goalText?: string): string {
  const who = resolveDisplayName(name);
  const goal = goalText?.trim();
  if (goal) {
    return `${who}, вы справитесь! ${baseAffirmation} Ваша цель — ${goal}.`;
  }
  return `${who}, вы справитесь! ${baseAffirmation}`;
}

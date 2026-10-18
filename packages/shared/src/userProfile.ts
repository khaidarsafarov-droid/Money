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
export const DEFAULT_GOAL_TEXT = "Накопить на важную цель";
const LEGACY_GOAL_TEXT = "Стать финансово свободным";

const NAME_PATTERN = /^[\p{L}][\p{L}\s'-]{1,}$/u;

export function isValidDisplayName(name: string): boolean {
  const trimmed = name.trim();
  return trimmed.length >= 2 && NAME_PATTERN.test(trimmed);
}

export function isValidAge(age: number | null | undefined): boolean {
  if (age == null) return true;
  return Number.isInteger(age) && age >= 18 && age <= 99;
}

const DEFAULT_DISPLAY_NAME_EN = "Friend";

export function defaultDisplayName(locale = "ru"): string {
  return locale.toLowerCase().startsWith("en") ? DEFAULT_DISPLAY_NAME_EN : DEFAULT_DISPLAY_NAME;
}

export function resolveDisplayName(name: string, locale = "ru"): string {
  const trimmed = name.trim();
  const placeholder =
    trimmed.length < 2 ||
    trimmed === DEFAULT_DISPLAY_NAME ||
    trimmed.toLowerCase() === DEFAULT_DISPLAY_NAME_EN.toLowerCase();
  return placeholder ? defaultDisplayName(locale) : trimmed;
}

export function resolveGoalText(goal: string): string {
  const trimmed = goal.trim();
  const placeholder =
    trimmed.length === 0 ||
    trimmed === DEFAULT_GOAL_TEXT ||
    trimmed === LEGACY_GOAL_TEXT ||
    trimmed.toLowerCase() === "become financially free";
  return placeholder ? DEFAULT_GOAL_TEXT : trimmed;
}

export function personalizedGreeting(
  name: string,
  _progressPercent = 0,
  locale = "ru",
): string {
  const who = resolveDisplayName(name, locale);
  if (locale.toLowerCase().startsWith("en")) {
    return `Hi, ${who}!`;
  }
  return `Привет, ${who}!`;
}

export function psychotypeOnboardingDescription(type: Psychotype): string {
  switch (type) {
    case "SPENDER":
      return "Вы любите тратить — это даёт вам энергию. Давайте направим её в нужное русло.";
    case "SAVER":
      return "Вы умеете копить — это ваша суперсила. Научимся тратить с удовольствием.";
    case "INDIFFERENT":
      return "Деньги не ваша страсть, но контроль над ними даст вам спокойствие.";
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

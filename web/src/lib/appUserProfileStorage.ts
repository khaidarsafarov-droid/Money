import {
  DEFAULT_USER_PROFILE,
  type UserProfile,
} from "@money/shared";

const USER_PROFILE_KEY = "money.userProfile.v1";

function readJson<T>(key: string, fallback: T): T {
  if (typeof localStorage === "undefined") return fallback;
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return fallback;
    return { ...fallback, ...JSON.parse(raw) } as T;
  } catch {
    return fallback;
  }
}

function writeJson<T>(key: string, value: T): void {
  if (typeof localStorage === "undefined") return;
  localStorage.setItem(key, JSON.stringify(value));
}

export function loadUserProfile(): UserProfile {
  return readJson(USER_PROFILE_KEY, DEFAULT_USER_PROFILE);
}

export function saveUserProfile(value: UserProfile): void {
  writeJson(USER_PROFILE_KEY, value);
}

export const HAS_COMPLETED_ONBOARDING_KEY = "hasCompletedOnboarding";

export function hasCompletedOnboarding(): boolean {
  return loadUserProfile().onboardingCompleted;
}

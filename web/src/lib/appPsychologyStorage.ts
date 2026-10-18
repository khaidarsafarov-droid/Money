import {
  DEFAULT_AUTOPILOT,
  DEFAULT_PRIVACY,
  type AdvisorMessage,
  type AutopilotSettings,
  type PrivacyPreferences,
} from "@money/shared";

const PRIVACY_KEY = "money.privacy.v1";
const AUTOPILOT_KEY = "money.autopilot.v1";
const ADVISOR_KEY = "money.advisor.v1";

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

export function loadPrivacyPreferences(): PrivacyPreferences {
  return readJson(PRIVACY_KEY, DEFAULT_PRIVACY);
}

export function savePrivacyPreferences(value: PrivacyPreferences): void {
  writeJson(PRIVACY_KEY, value);
}

export function loadAutopilotSettings(): AutopilotSettings {
  return readJson(AUTOPILOT_KEY, DEFAULT_AUTOPILOT);
}

export function saveAutopilotSettings(value: AutopilotSettings): void {
  writeJson(AUTOPILOT_KEY, value);
}

export function loadAdvisorHistory(): AdvisorMessage[] {
  return readJson<AdvisorMessage[]>(ADVISOR_KEY, []);
}

export function saveAdvisorHistory(messages: AdvisorMessage[]): void {
  writeJson(ADVISOR_KEY, messages.slice(-20));
}

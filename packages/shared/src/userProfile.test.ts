import { describe, expect, it } from "vitest";
import {
  DEFAULT_DISPLAY_NAME,
  isValidDisplayName,
  isValidAge,
  personalizedGreeting,
  resolveDisplayName,
  psychotypeOnboardingDescription,
} from "./userProfile";

describe("userProfile", () => {
  it("validates display name", () => {
    expect(isValidDisplayName("А")).toBe(false);
    expect(isValidDisplayName("Алекс")).toBe(true);
  });

  it("falls back to default name", () => {
    expect(resolveDisplayName("")).toBe(DEFAULT_DISPLAY_NAME);
    expect(resolveDisplayName("Мария")).toBe("Мария");
  });

  it("validates age range", () => {
    expect(isValidAge(17)).toBe(false);
    expect(isValidAge(30)).toBe(true);
    expect(isValidAge(null)).toBe(true);
  });

  it("builds personalized greeting", () => {
    expect(personalizedGreeting("Иван", 20)).toContain("Иван");
    expect(personalizedGreeting("Иван", 20)).toContain("20%");
    expect(personalizedGreeting("", 0, "en")).toContain("Friend");
    expect(personalizedGreeting("Ivan", 20, "en-US")).toContain("closer to freedom");
  });

  it("returns psychotype onboarding copy", () => {
    expect(psychotypeOnboardingDescription("SAVER")).toContain("копить");
  });
});

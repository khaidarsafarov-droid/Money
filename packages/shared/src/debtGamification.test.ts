import { describe, expect, it } from "vitest";
import {
  applyPaymentGamification,
  BADGE_FIRST_STEP,
  BADGE_STREAK_3,
  EMPTY_GAMIFICATION,
} from "./debtGamification";

describe("debtGamification", () => {
  it("unlocks first step badge on payment", () => {
    const next = applyPaymentGamification(
      EMPTY_GAMIFICATION,
      { paidAtMs: Date.UTC(2026, 0, 10), amountCents: 500 },
      5_000,
      10_000,
      1,
      Date.UTC(2026, 0, 10),
    );
    expect(next.unlockedBadges).toContain(BADGE_FIRST_STEP);
    expect(next.streakCount).toBe(1);
  });

  it("extends streak on consecutive days", () => {
    const day1 = applyPaymentGamification(
      EMPTY_GAMIFICATION,
      { paidAtMs: Date.UTC(2026, 0, 10), amountCents: 500 },
      1_000,
      10_000,
      1,
      Date.UTC(2026, 0, 10),
    );
    const day2 = applyPaymentGamification(
      day1,
      { paidAtMs: Date.UTC(2026, 0, 11), amountCents: 500 },
      2_000,
      10_000,
      2,
      Date.UTC(2026, 0, 11),
    );
    expect(day2.streakCount).toBe(2);
  });

  it("unlocks streak badge at 3", () => {
    let state = EMPTY_GAMIFICATION;
    for (let day = 1; day <= 3; day++) {
      state = applyPaymentGamification(
        state,
        { paidAtMs: Date.UTC(2026, 0, day), amountCents: 500 },
        day * 1_000,
        10_000,
        day,
        Date.UTC(2026, 0, day),
      );
    }
    expect(state.unlockedBadges).toContain(BADGE_STREAK_3);
  });
});

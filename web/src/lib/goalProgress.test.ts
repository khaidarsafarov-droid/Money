import { describe, expect, it } from "vitest";
import type { FinancialItem } from "@/types/finance";
import {
  dailyGoalCents,
  displayProgressFraction,
  isGoalOverAchieved,
  progressPercentRaw,
} from "@/lib/goalProgress";

const goal: FinancialItem = {
  id: "g1",
  type: "GOAL",
  name: "Truck",
  status: "PARTIAL",
  amount: 50_000,
  target: 100_000,
  history: [],
};

describe("goalProgress", () => {
  it("shows raw percent above 100", () => {
    const over: FinancialItem = { ...goal, amount: 125_000, status: "COMPLETED" };
    expect(progressPercentRaw(over)).toBe(125);
    expect(displayProgressFraction(over)).toBe(1);
    expect(isGoalOverAchieved(over)).toBe(true);
  });

  it("calculates daily goal from deadline", () => {
    const withDeadline: FinancialItem = {
      ...goal,
      dateDeadline: "2026-06-11",
    };
    const daily = dailyGoalCents(withDeadline, new Date("2026-06-01T12:00:00Z").getTime());
    expect(daily).toBe(5000);
  });
});

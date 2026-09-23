import { describe, expect, it } from "vitest";
import type { FinancialItem } from "@/types/finance";
import {
  dailyGoalCents,
  displayProgressFraction,
  isGoalOverAchieved,
  progressPercentRaw,
  summarizeGoals,
} from "@/lib/goalProgress";
import { sortItems } from "@/lib/financeEngine";

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

  it("summarizes active goals and sorts overdue first", () => {
    const trip: FinancialItem = { ...goal, id: "trip", name: "Trip", dateDeadline: "2026-06-11" };
    const late: FinancialItem = { ...goal, id: "late", name: "Late", dateDeadline: "2026-05-01" };
    const done: FinancialItem = {
      ...goal,
      id: "done",
      name: "Done",
      amount: 100_000,
      status: "COMPLETED",
      dateDeadline: "2026-06-02",
    };
    const now = new Date("2026-06-01T12:00:00Z").getTime();
    const summary = summarizeGoals([trip, late, done], now);
    expect(summary.activeCount).toBe(2);
    expect(summary.savedCents).toBe(100_000);
    expect(summary.targetCents).toBe(200_000);
    expect(summary.overdueCount).toBe(1);
    expect(summary.progressPercent).toBe(50);
    expect(summary.dailyCents).toBe(5000);

    const sorted = sortItems([done, trip, late]);
    expect(sorted.map((item) => item.name)).toEqual(["Late", "Trip", "Done"]);
  });
});

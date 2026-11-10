import { describe, expect, it } from "vitest";
import type { FinancialItem, Transaction } from "@/types/finance";
import { findOpenGoal, goalActivity, goalDetailCapabilities } from "@/lib/goalDetail";

const moneyGoal: FinancialItem = {
  id: "trip",
  type: "GOAL",
  name: "Поездка",
  status: "PARTIAL",
  amount: 20_000,
  target: 100_000,
  history: [],
};

const taskGoal: FinancialItem = {
  ...moneyGoal,
  id: "steps",
  name: "Шаги",
  goalKind: "ACTION",
  amount: 0,
  target: 0,
};

describe("goalDetail", () => {
  it("opens a live goal and ignores deleted or other types", () => {
    const debt: FinancialItem = { ...moneyGoal, id: "d1", type: "DEBT" };
    const deleted: FinancialItem = { ...moneyGoal, id: "gone", deleted: true };
    expect(findOpenGoal([moneyGoal, taskGoal, debt, deleted], "trip")?.name).toBe("Поездка");
    expect(findOpenGoal([moneyGoal, deleted], "gone")).toBeNull();
    expect(findOpenGoal([debt], "d1")).toBeNull();
    expect(findOpenGoal([moneyGoal], null)).toBeNull();
  });

  it("shows money actions only on savings goals", () => {
    expect(goalDetailCapabilities(moneyGoal)).toEqual({
      isTaskGoal: false,
      canTransfer: true,
      canExpense: true,
    });
    expect(goalDetailCapabilities({ ...moneyGoal, status: "COMPLETED" })).toEqual({
      isTaskGoal: false,
      canTransfer: false,
      canExpense: true,
    });
    expect(goalDetailCapabilities(taskGoal)).toEqual({
      isTaskGoal: true,
      canTransfer: false,
      canExpense: false,
    });
  });

  it("lists transfers and expenses for the open goal", () => {
    const txs: Transaction[] = [
      { id: "t1", kind: "GOAL_TRANSFER", amount: 1000, goalId: "trip", createdAt: "2026-11-01T00:00:00.000Z" },
      { id: "t2", kind: "EXPENSE", amount: 250, goalId: "trip", createdAt: "2026-11-01T01:00:00.000Z" },
      { id: "t3", kind: "GOAL_TRANSFER", amount: 500, goalId: "other", createdAt: "2026-11-01T02:00:00.000Z" },
      { id: "t4", kind: "DEPOSIT", amount: 9000, createdAt: "2026-11-01T03:00:00.000Z" },
      { id: "t5", kind: "EXPENSE", amount: 10, goalId: "trip", createdAt: "2026-11-01T04:00:00.000Z", deleted: true },
    ];
    expect(goalActivity(txs, "trip").map((tx) => tx.id)).toEqual(["t1", "t2"]);
  });
});

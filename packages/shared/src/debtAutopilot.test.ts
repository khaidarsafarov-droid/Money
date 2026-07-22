import { describe, expect, it } from "vitest";
import { pickTargetDebt, planMicroPayment } from "./debtAutopilot";

describe("debtAutopilot", () => {
  const debts = [
    {
      id: "a",
      name: "Tiny",
      remainingCents: 2_000,
      targetCents: 5_000,
      isCompleted: false,
    },
    {
      id: "b",
      name: "Big",
      remainingCents: 40_000,
      targetCents: 50_000,
      isCompleted: false,
      dateDueMs: Date.UTC(2026, 5, 1),
    },
  ];

  it("snowball picks smallest remaining", () => {
    expect(pickTargetDebt(debts, "snowball")?.id).toBe("a");
  });

  it("plans micro payment when enabled", () => {
    const plan = planMicroPayment(debts, {
      enabled: true,
      dailyAmountCents: 300,
      strategy: "snowball",
      reminderHour: 20,
      reminderMinute: 0,
      roundPaymentsToTen: false,
    });
    expect(plan?.debtId).toBe("a");
    expect(plan?.amountCents).toBe(300);
  });
});

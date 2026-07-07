import { describe, expect, it } from "vitest";
import { pickTargetDebt } from "./debtAutopilot";
import { calculateExtraPaymentImpact, formatExtraPaymentHint } from "./debtExtraPayment";
import { isHighPriorityLoanCategory } from "./loanCategory";

describe("pickTargetDebt avalanche by interest", () => {
  it("prefers highest interest rate", () => {
    const target = pickTargetDebt(
      [
        {
          id: "1",
          name: "Mortgage",
          remainingCents: 100_000,
          targetCents: 100_000,
          isCompleted: false,
          interestRateBps: 500,
        },
        {
          id: "2",
          name: "Card",
          remainingCents: 5_000,
          targetCents: 5_000,
          isCompleted: false,
          interestRateBps: 2_400,
        },
      ],
      "avalanche",
    );
    expect(target?.name).toBe("Card");
  });
});

describe("loanCategory", () => {
  it("marks credit card as high priority", () => {
    expect(isHighPriorityLoanCategory("CREDIT_CARD")).toBe(true);
    expect(isHighPriorityLoanCategory("MORTGAGE")).toBe(false);
  });
});

describe("debtExtraPayment", () => {
  it("calculates days saved with extra payment", () => {
    const impact = calculateExtraPaymentImpact({
      remainingCents: 25_000,
      monthlyPaymentCents: 10_000,
      extraPaymentCents: 5_000,
      interestRateBps: 1_200,
    });
    expect(impact.daysSaved).toBeGreaterThan(0);
    const hint = formatExtraPaymentHint(impact, 5_000);
    expect(hint).toContain("+$50");
  });
});

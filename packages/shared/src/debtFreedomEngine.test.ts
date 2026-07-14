import { describe, expect, it } from "vitest";
import {
  calculateFreedomProjection,
  computeDebtMilestones,
  estimatePaymentsToClose,
  isSmallDebt,
  suggestAcceleratePaymentCents,
} from "./debtFreedomEngine";

describe("computeDebtMilestones", () => {
  it("returns next milestone at 25% when at 10%", () => {
    const result = computeDebtMilestones(2_000, 20_000);
    expect(result.progressPercent).toBe(10);
    expect(result.nextMilestone?.percent).toBe(25);
    expect(result.nextMilestone?.centsToReach).toBe(3_000);
  });

  it("marks 100% when fully paid", () => {
    const result = computeDebtMilestones(20_000, 20_000);
    expect(result.nextMilestone).toBeNull();
    expect(result.milestones.every((m) => m.reached)).toBe(true);
  });
});

describe("suggestAcceleratePaymentCents", () => {
  it("suggests at least $5", () => {
    expect(suggestAcceleratePaymentCents(50_000, 100_000, 50_000)).toBeGreaterThanOrEqual(500);
  });
});

describe("calculateFreedomProjection", () => {
  it("returns no active debts when all completed", () => {
    const projection = calculateFreedomProjection([
      {
        remainingCents: 0,
        targetCents: 10_000,
        currentCents: 10_000,
        isCompleted: true,
        payments: [],
      },
    ]);
    expect(projection.hasActiveDebts).toBe(false);
    expect(projection.freedomDateMs).toBeNull();
  });

  it("projects freedom date from remaining and payments", () => {
    const now = Date.UTC(2026, 0, 1);
    const projection = calculateFreedomProjection(
      [
        {
          remainingCents: 12_000,
          targetCents: 20_000,
          currentCents: 8_000,
          isCompleted: false,
          payments: [{ amountCents: 4_000, paidAtMs: now - 30 * 86_400_000 }],
        },
      ],
      0,
      now,
    );
    expect(projection.hasActiveDebts).toBe(true);
    expect(projection.freedomDateMs).not.toBeNull();
    expect(projection.monthsRemaining).toBeGreaterThan(0);
  });

  it("respects due date over historical payment pace", () => {
    const now = Date.UTC(2026, 6, 7);
    const due = Date.UTC(2026, 6, 31);
    const projection = calculateFreedomProjection(
      [
        {
          remainingCents: 35_000,
          targetCents: 50_000,
          currentCents: 15_000,
          isCompleted: false,
          payments: [{ amountCents: 15_000, paidAtMs: now }],
          dateDueMs: due,
        },
      ],
      0,
      now,
    );

    expect(projection.freedomDateMs).toBe(due);
    expect(projection.monthlyPaymentCents).toBe(35_000);
    expect(projection.monthsRemaining).toBe(1);
  });

  it("shortens timeline with extra monthly payment", () => {
    const now = Date.UTC(2026, 0, 1);
    const debt = {
      remainingCents: 24_000,
      targetCents: 30_000,
      currentCents: 6_000,
      isCompleted: false,
      payments: [{ amountCents: 2_000, paidAtMs: now - 60 * 86_400_000 }],
      dateDueMs: now + 180 * 86_400_000,
    };
    const base = calculateFreedomProjection([debt], 0, now);
    const extra = calculateFreedomProjection([debt], 2_000, now);
    expect(extra.monthsRemaining!).toBeLessThanOrEqual(base.monthsRemaining!);
    expect(extra.monthsSavedWithExtra).toBeGreaterThan(0);
  });
});

describe("isSmallDebt", () => {
  it("detects debts under $100", () => {
    expect(isSmallDebt(5_000)).toBe(true);
    expect(isSmallDebt(15_000)).toBe(false);
  });
});

describe("estimatePaymentsToClose", () => {
  it("ceil-divides remaining by payment", () => {
    expect(estimatePaymentsToClose(10_000, 3_000)).toBe(4);
  });
});

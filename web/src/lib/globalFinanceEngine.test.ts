import { describe, expect, it } from "vitest";
import { addGoal } from "@/lib/financeEngine";
import {
  addTransaction,
  computeActiveGoalMetrics,
  computeGlobalFinanceSummary,
  enrichFinanceState,
  migrateLegacyGoalHistory,
} from "@/lib/globalFinanceEngine";
import type { FinanceState } from "@/types/finance";

const emptyState: FinanceState = { currency: "USD", items: [], transactions: [] };

describe("computeGlobalFinanceSummary", () => {
  it("derives global balance as income minus expenses", () => {
    const withGoal = addGoal(emptyState, { name: "Trip", target: 100_000 });
    const goalId = withGoal.items[0]!.id;

    let state = addTransaction(withGoal, { kind: "DEPOSIT", amountCents: 50_000 }).state!;
    state = addTransaction(state, { kind: "GOAL_TRANSFER", amountCents: 20_000, goalId }).state!;
    state = addTransaction(state, { kind: "EXPENSE", amountCents: 5_000 }).state!;

    const summary = computeGlobalFinanceSummary(state);
    expect(summary.globalBalanceCents).toBe(45_000);
    expect(summary.availableBalanceCents).toBe(45_000);
    expect(summary.totalAccumulatedCents).toBe(20_000);
  });

  it("lets a goal contribution succeed when the free wallet is empty", () => {
    const withGoal = addGoal(emptyState, { name: "Trip", target: 100_000 });
    const goalId = withGoal.items[0]!.id;
    const result = addTransaction(withGoal, { kind: "GOAL_TRANSFER", amountCents: 20_000, goalId });
    expect(result.error).toBeUndefined();
    const summary = computeGlobalFinanceSummary(result.state);
    expect(summary.availableBalanceCents).toBe(0);
    expect(summary.totalAccumulatedCents).toBe(20_000);
  });

  it("spends a goal expense only from that goal", () => {
    const withGoal = addGoal(emptyState, { name: "Trip", target: 100_000 });
    const goalId = withGoal.items[0]!.id;
    let state = addTransaction(withGoal, { kind: "DEPOSIT", amountCents: 6_000 }).state!;
    state = addTransaction(state, { kind: "GOAL_TRANSFER", amountCents: 5_000, goalId }).state!;
    state = addTransaction(state, {
      kind: "EXPENSE",
      amountCents: 1_000,
      goalId,
      category: "from goal",
    }).state!;

    const summary = computeGlobalFinanceSummary(state);
    expect(summary.availableBalanceCents).toBe(6_000);
    expect(summary.totalAccumulatedCents).toBe(4_000);
  });

  it("blocks spending below zero available balance", () => {
    const state = addTransaction(emptyState, { kind: "DEPOSIT", amountCents: 1_000 }).state!;
    const blocked = addTransaction(state, { kind: "EXPENSE", amountCents: 2_000 });
    expect(blocked.error).toBeTruthy();
    expect(blocked.state).toBe(state);
  });

  it("keeps sync tombstones out of financial totals", () => {
    const state: FinanceState = {
      ...emptyState,
      transactions: [{
        id: "deleted-deposit",
        kind: "DEPOSIT",
        amount: 10_000,
        createdAt: "2026-01-01T00:00:00.000Z",
        deleted: true,
      }],
    };

    expect(computeGlobalFinanceSummary(state).globalBalanceCents).toBe(0);
  });
});

describe("computeActiveGoalMetrics", () => {
  it("sums remaining only for active goals", () => {
    const withGoals = addGoal(
      addGoal(emptyState, { name: "A", target: 100_000 }),
      { name: "B", target: 50_000 },
    );
    const goalA = withGoals.items.find((item) => item.name === "A")!.id;
    let state = addTransaction(withGoals, { kind: "DEPOSIT", amountCents: 200_000 }).state!;
    state = addTransaction(state, { kind: "GOAL_TRANSFER", amountCents: 40_000, goalId: goalA }).state!;

    const enriched = enrichFinanceState(state);
    const metrics = computeActiveGoalMetrics(enriched.items);

    expect(metrics.totalAccumulatedCents).toBe(40_000);
    expect(metrics.totalRemainingToGoalCents).toBe(110_000);
  });

  it("leaves action goals out of money totals", () => {
    const state: FinanceState = {
      currency: "USD",
      items: [{
        id: "steps",
        type: "GOAL",
        name: "Steps",
        status: "ACTIVE",
        amount: 50_000,
        target: 99_000,
        history: [],
        goalKind: "ACTION",
      }],
      transactions: [{
        id: "move",
        kind: "GOAL_TRANSFER",
        amount: 50_000,
        goalId: "steps",
        createdAt: "2026-01-01T00:00:00.000Z",
      }],
    };

    const enriched = enrichFinanceState(state);
    expect(enriched.items[0]?.amount).toBe(0);
    expect(computeActiveGoalMetrics(enriched.items).totalAccumulatedCents).toBe(0);
  });
});

describe("migrateLegacyGoalHistory", () => {
  it("keeps migrated savings on the goal wallet", () => {
    const legacy = addGoal(emptyState, { name: "Old", target: 100_000 });
    legacy.items[0]!.amount = 30_000;

    const migrated = migrateLegacyGoalHistory(legacy);
    expect(migrated.transactions.some((tx) => tx.kind === "GOAL_TRANSFER")).toBe(true);
    expect(migrated.transactions.some((tx) => tx.kind === "DEPOSIT")).toBe(false);
    const summary = computeGlobalFinanceSummary(migrated);
    expect(summary.availableBalanceCents).toBe(0);
    expect(summary.totalAccumulatedCents).toBe(30_000);
  });
});

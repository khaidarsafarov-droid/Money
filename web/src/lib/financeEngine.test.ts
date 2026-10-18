import { describe, expect, it } from "vitest";
import {
  addDebt,
  addDebtAdvance,
  addGoal,
  debtRemainingCents,
  filterByType,
  filterItems,
  isDebtOverdue,
  planProgressPercent,
  processPayment,
  remainingAmount,
  removeItem,
  resolveStatus,
  sortItems,
} from "@/lib/financeEngine";
import { addTransaction } from "@/lib/globalFinanceEngine";
import type { FinanceState } from "@/types/finance";

const emptyState: FinanceState = { currency: "USD", items: [], transactions: [] };

describe("planProgressPercent", () => {
  it("combines paid debts with goal savings", () => {
    expect(
      planProgressPercent({
        debtPaidCents: 5_000,
        debtTargetCents: 10_000,
        goalAccumulatedCents: 2_000,
        goalRemainingCents: 2_000,
      }),
    ).toBe(50);
  });

  it("is zero when there is nothing to measure", () => {
    expect(
      planProgressPercent({
        debtPaidCents: 0,
        debtTargetCents: 0,
        goalAccumulatedCents: 0,
        goalRemainingCents: 0,
      }),
    ).toBe(0);
  });

  it("truncates toward zero so 2/3 is 66", () => {
    expect(
      planProgressPercent({
        debtPaidCents: 2,
        debtTargetCents: 3,
        goalAccumulatedCents: 0,
        goalRemainingCents: 0,
      }),
    ).toBe(66);
  });

  it("caps the ratio at 100", () => {
    expect(
      planProgressPercent({
        debtPaidCents: 15_000,
        debtTargetCents: 10_000,
        goalAccumulatedCents: 0,
        goalRemainingCents: 0,
      }),
    ).toBe(100);
  });
});

describe("resolveStatus", () => {
  it("returns ACTIVE when nothing paid", () => {
    expect(resolveStatus(10_000, 0)).toBe("ACTIVE");
  });

  it("returns PARTIAL when partially paid", () => {
    expect(resolveStatus(10_000, 4_000)).toBe("PARTIAL");
  });

  it("returns COMPLETED when fully paid", () => {
    expect(resolveStatus(10_000, 10_000)).toBe("COMPLETED");
  });
});

describe("processPayment", () => {
  it("updates amount, history, and status", () => {
    const withDebt = addDebt(emptyState, {
      name: "Alex",
      target: 5_000,
      dateGiven: "2026-01-01",
      dateDue: "2026-06-01",
    });
    const debtId = withDebt.items[0]!.id;

    const partial = processPayment(withDebt, debtId, 2_000);
    const item = partial.items[0]!;

    expect(item.amount).toBe(2_000);
    expect(item.status).toBe("PARTIAL");
    expect(item.history).toHaveLength(1);
    expect(item.history[0]?.amount).toBe(2_000);
    expect(remainingAmount(item)).toBe(3_000);
  });

  it("caps payment at remaining balance", () => {
    const withDebt = addDebt(emptyState, {
      name: "Alex",
      target: 5_000,
      dateGiven: "2026-01-01",
      dateDue: "2026-06-01",
    });
    const debtId = withDebt.items[0]!.id;

    const completed = processPayment(withDebt, debtId, 9_999);
    const item = completed.items[0]!;

    expect(item.amount).toBe(5_000);
    expect(item.status).toBe("COMPLETED");
    expect(item.history[0]?.amount).toBe(5_000);
  });

  it("completes goal contributions when funds are available", () => {
    const withGoal = addGoal(emptyState, { name: "Truck", target: 1_000_000 });
    const goalId = withGoal.items[0]!.id;

    const funded = addTransaction(withGoal, { kind: "DEPOSIT", amountCents: 1_000_000 }).state!;
    const done = processPayment(funded, goalId, 1_000_000);
    expect(done.items[0]?.status).toBe("COMPLETED");
  });
});

describe("filterItems", () => {
  const withGoal = addGoal(
    addDebt(emptyState, {
      name: "Debt",
      target: 100,
      dateGiven: "2026-01-01",
      dateDue: "2026-06-01",
    }),
    { name: "Goal", target: 100 },
  );
  const goalId = withGoal.items.find((item) => item.type === "GOAL")!.id;
  const funded = addTransaction(withGoal, { kind: "DEPOSIT", amountCents: 100 }).state!;
  const state = processPayment(funded, goalId, 100);

  it("filters active items", () => {
    expect(filterItems(state.items, "active")).toHaveLength(1);
  });

  it("filters completed items", () => {
    expect(filterItems(state.items, "completed")).toHaveLength(1);
  });
});

describe("filterByType and sortItems", () => {
  it("filters goals only", () => {
    const state = addGoal(emptyState, { name: "Goal", target: 100 });
    expect(filterByType(state.items, "goal")).toHaveLength(1);
    expect(filterByType(state.items, "debt")).toHaveLength(0);
  });

  it("sorts overdue debts first", () => {
    const overdue = addDebt(emptyState, {
      name: "Late",
      target: 100,
      dateGiven: "2025-01-01",
      dateDue: "2025-01-02",
    });
    const fresh = addDebt(overdue, {
      name: "Fresh",
      target: 100,
      dateGiven: "2026-01-01",
      dateDue: "2027-01-01",
    });
    const sorted = sortItems(fresh.items);
    expect(sorted[0]?.name).toBe("Late");
    expect(isDebtOverdue(sorted[0]!)).toBe(true);
  });
});

describe("removeItem", () => {
  it("soft-deletes item with tombstone instead of removing from array", () => {
    const withDebt = addDebt(emptyState, {
      name: "Alex",
      target: 5_000,
      dateGiven: "2026-01-01",
      dateDue: "2026-06-01",
    });
    const debtId = withDebt.items[0]!.id;
    const next = removeItem(withDebt, debtId);

    expect(next.items).toHaveLength(1);
    expect(next.items[0]?.deleted).toBe(true);
    expect(filterItems(next.items, "all")).toHaveLength(0);
  });
});

describe("addDebtAdvance", () => {
  it("adds another disbursement to the same debt and keeps the remaining balance", () => {
    const created = addDebt(emptyState, {
      name: "Isrofil",
      target: 150_000,
      dateGiven: "2026-10-24",
      dateDue: "2026-11-24",
    });
    const debtId = created.items[0]!.id;
    const next = addDebtAdvance(created, debtId, 20_000, "2026-11-01", "ещё");
    const debt = next.items[0]!;
    expect(debt.id).toBe(debtId);
    expect(debt.target).toBe(170_000);
    expect(debt.advances).toHaveLength(2);
    expect(debt.advances?.[1]?.note).toBe("ещё");
    expect(
      debtRemainingCents(
        debt.advances!.map((advance) => advance.amount),
        debt.history.map((payment) => payment.amount),
      ),
    ).toBe(170_000);
  });
});

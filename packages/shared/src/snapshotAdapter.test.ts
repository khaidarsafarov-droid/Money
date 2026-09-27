import { describe, expect, it } from "vitest";
import { financeStateToSnapshot, snapshotToFinanceState } from "./snapshotAdapter";
import type { FinanceState } from "./finance";

describe("snapshotAdapter", () => {
  it("round-trips goals, debts, and transactions", () => {
    const state: FinanceState = {
      currency: "USD",
      items: [
        {
          id: "goal-a",
          type: "GOAL",
          name: "Резерв",
          status: "ACTIVE",
          amount: 500,
          target: 2_000,
          history: [],
          updatedAt: "2026-06-01T10:00:00.000Z",
        },
        {
          id: "debt-b",
          type: "DEBT",
          name: "Алексей",
          status: "PARTIAL",
          amount: 300,
          target: 1_000,
          history: [{ id: "pay-1", amount: 300, paidAt: "2026-06-02T12:00:00.000Z" }],
          dateGiven: "2026-05-01",
          dateDue: "2026-07-01",
          updatedAt: "2026-06-02T12:00:00.000Z",
        },
      ],
      transactions: [
        {
          id: "tx-deposit",
          kind: "DEPOSIT",
          amount: 2_000,
          createdAt: "2026-06-01T09:00:00.000Z",
        },
        {
          id: "tx-goal",
          kind: "GOAL_TRANSFER",
          amount: 500,
          goalId: "goal-a",
          createdAt: "2026-06-01T10:00:00.000Z",
        },
      ],
    };

    const snapshot = financeStateToSnapshot(state, "web-device");
    const restored = snapshotToFinanceState(snapshot);

    expect(restored.items).toHaveLength(2);
    expect(restored.transactions).toHaveLength(2);
    expect(restored.items.find((item) => item.id === "goal-a")?.name).toBe("Резерв");
    expect(restored.items.find((item) => item.id === "debt-b")?.history).toHaveLength(1);
  });

  it("includes tombstoned items in snapshot for sync", () => {
    const state: FinanceState = {
      currency: "USD",
      items: [
        {
          id: "goal-deleted",
          type: "GOAL",
          name: "Old goal",
          status: "ACTIVE",
          amount: 0,
          target: 1_000,
          history: [],
          deleted: true,
          updatedAt: "2026-06-03T10:00:00.000Z",
        },
      ],
      transactions: [],
    };

    const snapshot = financeStateToSnapshot(state, "web-device");
    expect(snapshot.goals).toHaveLength(1);
    expect(snapshot.goals[0]?.deleted).toBe(true);
    const restored = snapshotToFinanceState(snapshot);
    expect(restored.items).toHaveLength(1);
    expect(restored.items[0]?.deleted).toBe(true);
  });

  it("retains payment and transaction tombstones through local state", () => {
    const state: FinanceState = {
      currency: "USD",
      items: [{
        id: "debt-1",
        type: "DEBT",
        name: "Debt",
        status: "ACTIVE",
        amount: 0,
        target: 1_000,
        dateGiven: "2026-01-01",
        history: [{
          id: "payment-1",
          amount: 100,
          paidAt: "2026-01-02T00:00:00.000Z",
          syncUpdatedAt: 200,
          deleted: true,
        }],
      }],
      transactions: [{
        id: "transaction-1",
        kind: "EXPENSE",
        amount: 100,
        createdAt: "2026-01-02T00:00:00.000Z",
        syncUpdatedAt: 200,
        deleted: true,
      }],
    };

    const first = financeStateToSnapshot(state, "web-device", 300);
    const second = financeStateToSnapshot(
      snapshotToFinanceState(first),
      "web-device",
      400,
    );

    expect(second.debtPayments[0]?.deleted).toBe(true);
    expect(second.debtPayments[0]?.syncUpdatedAt).toBe(200);
    expect(second.transactions[0]?.deleted).toBe(true);
    expect(second.transactions[0]?.syncUpdatedAt).toBe(200);
  });
});

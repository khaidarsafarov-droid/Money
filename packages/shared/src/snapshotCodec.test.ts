import { describe, expect, it } from "vitest";
import { SYNC_SCHEMA_VERSION } from "./constants";
import { decodeFinanceSnapshot } from "./snapshotCodec";

describe("decodeFinanceSnapshot", () => {
  const validSnapshot = {
    schemaVersion: SYNC_SCHEMA_VERSION,
    deviceId: "device-1",
    updatedAt: Date.now(),
    goals: [],
    debts: [],
    debtPayments: [],
    transactions: [],
  };

  it("accepts a valid snapshot", () => {
    const decoded = decodeFinanceSnapshot(JSON.stringify(validSnapshot));
    expect(decoded.deviceId).toBe("device-1");
  });

  it("migrates schema v2 debt fields to schema v3 defaults", () => {
    const version2 = {
      ...validSnapshot,
      schemaVersion: 2,
      debts: [{
        id: "debt-1",
        name: "Debt",
        targetAmountCents: 1_000,
        currentAmountCents: 0,
        currency: "USD",
        status: "ACTIVE",
        dateGiven: "2026-01-01",
        syncUpdatedAt: 100,
      }],
      debtPayments: [{
        id: "payment-1",
        debtorId: "debt-1",
        amountCents: 100,
        paidAt: "2026-01-02T00:00:00.000Z",
        syncUpdatedAt: 100,
      }],
    };

    const decoded = decodeFinanceSnapshot(JSON.stringify(version2));

    expect(decoded.schemaVersion).toBe(3);
    expect(decoded.debts[0]).toMatchObject({
      debtType: "CREDITOR",
      affectsBalance: false,
      note: "",
    });
    expect(decoded.debtPayments[0]?.affectsBalance).toBe(true);
  });

  it("keeps a legacy expense goal and goal kind when they are present", () => {
    const legacy = {
      schemaVersion: 1,
      deviceId: "device-1",
      updatedAt: 1,
      goals: [
        {
          id: 4,
          name: "Steps",
          targetAmountCents: 0,
          currency: "USD",
          syncUpdatedAt: 10,
          goalKind: "ACTION",
        },
        {
          id: 5,
          name: "Trip",
          targetAmountCents: 100,
          currency: "USD",
          syncUpdatedAt: 10,
        },
      ],
      debts: [],
      debtPayments: [],
      expenses: [{
        id: 9,
        amount: 1.5,
        category: "food",
        date: 1_700_000_000_000,
        isConfirmed: true,
        syncUpdatedAt: 11,
        goalId: "goal-sync",
      }],
    };

    const decoded = decodeFinanceSnapshot(JSON.stringify(legacy));
    expect(decoded.goals[0]?.goalKind).toBe("ACTION");
    expect(decoded.goals[1]?.goalKind).toBeUndefined();
    expect(decoded.transactions[0]?.goalId).toBe("goal-sync");
    expect(decoded.notes).toBeUndefined();
  });

  it("rejects invalid JSON", () => {
    expect(() => decodeFinanceSnapshot("{not json")).toThrow("Invalid snapshot JSON");
  });

  it("rejects missing deviceId", () => {
    const payload = { ...validSnapshot, deviceId: "" };
    expect(() => decodeFinanceSnapshot(JSON.stringify(payload))).toThrow("deviceId");
  });

  it("rejects oversized payloads", () => {
    const huge = "x".repeat(6 * 1024 * 1024);
    expect(() => decodeFinanceSnapshot(huge)).toThrow("maximum size");
  });

  it("rejects non-array goals field", () => {
    const payload = { ...validSnapshot, goals: {} };
    expect(() => decodeFinanceSnapshot(JSON.stringify(payload))).toThrow("goals");
  });
});

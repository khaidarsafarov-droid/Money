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

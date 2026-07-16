import { describe, expect, it } from "vitest";
import { decodeFinanceSnapshot } from "./snapshotCodec";

describe("decodeFinanceSnapshot", () => {
  const validSnapshot = {
    schemaVersion: 2,
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

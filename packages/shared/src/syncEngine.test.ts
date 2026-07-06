import { describe, expect, it } from "vitest";
import { mergeFinanceSnapshots, mergeFinanceSnapshotsSymmetric, mergeVoiceSettings } from "./syncEngine";
import type { FinanceSnapshot, SnapshotGoal } from "./sync";

const deviceA = "device-a";
const deviceB = "device-b";

function goal(
  id: string,
  name: string,
  syncUpdatedAt: number,
  deleted = false,
): SnapshotGoal {
  return {
    id,
    name,
    targetAmountCents: 1_000,
    currency: "USD",
    syncUpdatedAt,
    deleted,
  };
}

function snapshot(
  deviceId = deviceA,
  goals: SnapshotGoal[] = [],
): FinanceSnapshot {
  return {
    schemaVersion: 2,
    deviceId,
    updatedAt: 0,
    goals,
    debts: [],
    debtPayments: [],
    transactions: [],
  };
}

describe("mergeFinanceSnapshots", () => {
  it("picks newer goal by syncUpdatedAt", () => {
    const local = snapshot(deviceA, [goal("1", "Local", 100)]);
    const remote = snapshot(deviceB, [goal("1", "Remote", 200)]);

    const merged = mergeFinanceSnapshots(local, remote);

    expect(merged.goals).toHaveLength(1);
    expect(merged.goals[0]?.name).toBe("Remote");
  });

  it("prefers remote on timestamp tie", () => {
    const local = snapshot(deviceA, [goal("1", "Local", 100)]);
    const remote = snapshot(deviceB, [goal("1", "Remote", 100)]);

    const merged = mergeFinanceSnapshots(local, remote);

    expect(merged.goals[0]?.name).toBe("Remote");
  });

  it("applies tombstone when newer than local record", () => {
    const local = snapshot(deviceA, [goal("1", "Active", 100)]);
    const remote = snapshot(deviceB, [goal("1", "Deleted", 200, true)]);

    const merged = mergeFinanceSnapshots(local, remote);

    expect(merged.goals).toHaveLength(0);
  });

  it("keeps local when tombstone is older", () => {
    const local = snapshot(deviceA, [goal("1", "Active", 300)]);
    const remote = snapshot(deviceB, [goal("1", "Deleted", 100, true)]);

    const merged = mergeFinanceSnapshots(local, remote);

    expect(merged.goals[0]?.name).toBe("Active");
  });
});

describe("mergeFinanceSnapshotsSymmetric", () => {
  it("returns the same result regardless of argument order", () => {
    const first = snapshot(deviceA, [goal("1", "A", 100)]);
    const second = snapshot(deviceB, [goal("1", "B", 200)]);

    expect(mergeFinanceSnapshotsSymmetric(first, second)).toEqual(
      mergeFinanceSnapshotsSymmetric(second, first),
    );
  });
});

describe("mergeVoiceSettings", () => {
  it("returns undefined when both sides are missing", () => {
    expect(mergeVoiceSettings(undefined, undefined)).toBeUndefined();
  });

  it("picks newer side", () => {
    const older = {
      enabled: false,
      reportHour: 8,
      reportMinute: 0,
      language: "EN",
      gender: "FEMALE",
      forceInSilentMode: false,
      syncUpdatedAt: 100,
    };
    const newer = { ...older, enabled: true, syncUpdatedAt: 200 };

    expect(mergeVoiceSettings(older, newer)).toEqual(newer);
    expect(mergeVoiceSettings(newer, older)).toEqual(newer);
  });
});

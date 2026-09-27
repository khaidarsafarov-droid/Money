import { describe, expect, it } from "vitest";
import { SYNC_SCHEMA_VERSION } from "./constants";
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
    schemaVersion: SYNC_SCHEMA_VERSION,
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

    expect(merged.goals).toEqual([goal("1", "Deleted", 200, true)]);
  });

  it("keeps local when tombstone is older", () => {
    const local = snapshot(deviceA, [goal("1", "Active", 300)]);
    const remote = snapshot(deviceB, [goal("1", "Deleted", 100, true)]);

    const merged = mergeFinanceSnapshots(local, remote);

    expect(merged.goals[0]?.name).toBe("Active");
  });

  it("applies local tombstone when newer than remote active record", () => {
    const local = snapshot(deviceA, [goal("1", "Deleted", 300, true)]);
    const remote = snapshot(deviceB, [goal("1", "Active", 100)]);

    const merged = mergeFinanceSnapshots(local, remote);

    expect(merged.goals).toEqual([goal("1", "Deleted", 300, true)]);
  });

  it("keeps record only on remote when local has no tombstone", () => {
    const local = snapshot(deviceA, []);
    const remote = snapshot(deviceB, [goal("1", "Remote only", 100)]);

    const merged = mergeFinanceSnapshots(local, remote);

    expect(merged.goals).toHaveLength(1);
    expect(merged.goals[0]?.name).toBe("Remote only");
  });

  it("retains a deletion across three sequential peers", () => {
    const deletedOnA = snapshot("device-a", [goal("1", "Deleted", 300, true)]);
    const staleOnB = snapshot("device-b", [goal("1", "Stale B", 100)]);
    const staleOnC = snapshot("device-c", [goal("1", "Stale C", 100)]);

    const afterAB = mergeFinanceSnapshotsSymmetric(deletedOnA, staleOnB);
    const afterBC = mergeFinanceSnapshotsSymmetric(afterAB, staleOnC);

    expect(afterAB.goals[0]?.deleted).toBe(true);
    expect(afterBC.goals[0]?.deleted).toBe(true);
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

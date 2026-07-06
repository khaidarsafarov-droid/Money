import type {
  FinanceSnapshot,
  LwwRecord,
  SnapshotVoiceSettings,
} from "./sync";

function pickLww<T extends LwwRecord>(local: T, remote: T): T {
  if (remote.syncUpdatedAt > local.syncUpdatedAt) return remote;
  if (local.syncUpdatedAt > remote.syncUpdatedAt) return local;
  return remote;
}

export function mergeVoiceSettings(
  local: SnapshotVoiceSettings | undefined,
  remote: SnapshotVoiceSettings | undefined,
): SnapshotVoiceSettings | undefined {
  if (!local) return remote;
  if (!remote) return local;
  return remote.syncUpdatedAt >= local.syncUpdatedAt ? remote : local;
}

export function mergeRecords<T extends LwwRecord & { id: string }>(
  local: T[],
  remote: T[],
): T[] {
  const merged = new Map<string, T>();

  for (const record of local) {
    if (!record.deleted) {
      merged.set(record.id, record);
    }
  }

  for (const remoteRecord of remote) {
    const existing = merged.get(remoteRecord.id);
    const winner = existing ? pickLww(existing, remoteRecord) : remoteRecord;
    if (winner.deleted) {
      merged.delete(remoteRecord.id);
    } else {
      merged.set(remoteRecord.id, winner);
    }
  }

  return [...merged.values()].sort((a, b) => a.id.localeCompare(b.id));
}

export function mergeFinanceSnapshots(
  local: FinanceSnapshot,
  remote: FinanceSnapshot,
): FinanceSnapshot {
  if (local.schemaVersion !== remote.schemaVersion) {
    throw new Error(
      `Schema mismatch: local=${local.schemaVersion}, remote=${remote.schemaVersion}`,
    );
  }

  return {
    schemaVersion: local.schemaVersion,
    deviceId: remote.deviceId,
    updatedAt: Math.max(local.updatedAt, remote.updatedAt),
    goals: mergeRecords(local.goals, remote.goals),
    debts: mergeRecords(local.debts, remote.debts),
    debtPayments: mergeRecords(local.debtPayments, remote.debtPayments),
    transactions: mergeRecords(local.transactions, remote.transactions),
    voiceSettings: mergeVoiceSettings(local.voiceSettings, remote.voiceSettings),
  };
}

/** Deterministic merge — both peers compute the same result. */
export function mergeFinanceSnapshotsSymmetric(
  first: FinanceSnapshot,
  second: FinanceSnapshot,
): FinanceSnapshot {
  const [local, remote] =
    first.deviceId <= second.deviceId ? [first, second] : [second, first];
  return mergeFinanceSnapshots(local, remote);
}

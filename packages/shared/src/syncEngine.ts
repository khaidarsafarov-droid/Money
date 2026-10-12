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
  const localById = new Map(local.map((record) => [record.id, record]));
  const remoteById = new Map(remote.map((record) => [record.id, record]));
  const allIds = new Set([...localById.keys(), ...remoteById.keys()]);

  for (const id of allIds) {
    const localRecord = localById.get(id);
    const remoteRecord = remoteById.get(id);

    if (localRecord && remoteRecord) {
      const winner = pickLww(localRecord, remoteRecord);
      merged.set(id, winner);
      continue;
    }

    const sole = localRecord ?? remoteRecord;
    if (sole) {
      merged.set(id, sole);
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
    ...optionalList("goalActions", mergeOptional(local.goalActions, remote.goalActions)),
    ...optionalList("notes", mergeOptional(local.notes, remote.notes)),
    ...optionalList("noteLines", mergeOptional(local.noteLines, remote.noteLines)),
  };
}

function mergeOptional<T extends LwwRecord & { id: string }>(
  local: T[] | undefined,
  remote: T[] | undefined,
): T[] | undefined {
  if (local === undefined) return remote;
  if (remote === undefined) return local;
  return mergeRecords(local, remote);
}

function optionalList<K extends "goalActions" | "notes" | "noteLines">(
  key: K,
  value: FinanceSnapshot[K],
): Partial<Pick<FinanceSnapshot, K>> {
  return value === undefined ? {} : { [key]: value };
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

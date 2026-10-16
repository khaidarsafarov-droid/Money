import { SYNC_SCHEMA_VERSION } from "./constants";
import type {
  FinanceSnapshot,
  SnapshotDebt,
  SnapshotDebtPayment,
  SnapshotGoal,
  SnapshotTransaction,
  SnapshotVoiceSettings,
} from "./sync";

const MAX_SNAPSHOT_BYTES = 5 * 1024 * 1024;

export function encodeFinanceSnapshot(snapshot: FinanceSnapshot): string {
  return JSON.stringify(snapshot);
}

export function decodeFinanceSnapshot(raw: string): FinanceSnapshot {
  if (raw.length > MAX_SNAPSHOT_BYTES) {
    throw new Error(`Snapshot exceeds maximum size of ${MAX_SNAPSHOT_BYTES} bytes`);
  }

  let json: unknown;
  try {
    json = JSON.parse(raw);
  } catch {
    throw new Error("Invalid snapshot JSON");
  }

  if (!json || typeof json !== "object") {
    throw new Error("Snapshot must be a JSON object");
  }

  const candidate = json as FinanceSnapshot;
  if (typeof candidate.schemaVersion !== "number") {
    throw new Error("Snapshot missing schemaVersion");
  }
  if (typeof candidate.deviceId !== "string" || candidate.deviceId.length === 0) {
    throw new Error("Snapshot missing deviceId");
  }

  if (candidate.schemaVersion >= 2) {
    validateSnapshotArrays(candidate);
    return candidate.schemaVersion < SYNC_SCHEMA_VERSION
      ? migrateVersion2Snapshot(candidate)
      : candidate;
  }
  return migrateLegacySnapshot(json as LegacyFinanceSnapshot);
}

function validateSnapshotArrays(snapshot: FinanceSnapshot): void {
  const arrays: Array<keyof FinanceSnapshot> = [
    "goals",
    "debts",
    "debtPayments",
    "transactions",
  ];
  for (const key of arrays) {
    const value = snapshot[key];
    if (!Array.isArray(value)) {
      throw new Error(`Snapshot field "${key}" must be an array`);
    }
    if (value.length > 10_000) {
      throw new Error(`Snapshot field "${key}" exceeds maximum record count`);
    }
  }
  for (const key of ["goalActions", "notes", "noteLines", "clients"] as const) {
    const value = snapshot[key];
    if (value === undefined) continue;
    if (!Array.isArray(value)) {
      throw new Error(`Snapshot field "${key}" must be an array`);
    }
    if (value.length > 10_000) {
      throw new Error(`Snapshot field "${key}" exceeds maximum record count`);
    }
  }
}

function migrateVersion2Snapshot(snapshot: FinanceSnapshot): FinanceSnapshot {
  return {
    ...snapshot,
    schemaVersion: SYNC_SCHEMA_VERSION,
    goals: snapshot.goals.map((goal) => ({
      ...goal,
      mode: goal.mode ?? "PERSONAL",
    })),
    debts: snapshot.debts.map((debt) => ({
      ...debt,
      debtType: debt.debtType ?? "CREDITOR",
      affectsBalance: debt.affectsBalance ?? false,
      note: debt.note ?? "",
      mode: debt.mode ?? "PERSONAL",
    })),
    debtPayments: snapshot.debtPayments.map((payment) => ({
      ...payment,
      affectsBalance: payment.affectsBalance ?? true,
      mode: payment.mode ?? "PERSONAL",
    })),
    transactions: snapshot.transactions.map((transaction) => ({
      ...transaction,
      mode: transaction.mode ?? "PERSONAL",
    })),
    notes: snapshot.notes?.map((note) => ({
      ...note,
      mode: note.mode ?? "PERSONAL",
    })),
  };
}

interface LegacyFinanceSnapshot {
  schemaVersion: number;
  deviceId: string;
  updatedAt: number;
  goals: LegacySnapshotGoal[];
  debts: LegacySnapshotDebt[];
  debtPayments: LegacySnapshotDebtPayment[];
  transfers?: LegacySnapshotTransfer[];
  expenses?: LegacySnapshotExpense[];
  transactions?: SnapshotTransaction[];
  voiceSettings?: SnapshotVoiceSettings;
}

interface LegacySnapshotGoal {
  id: number | string;
  name: string;
  targetAmountCents: number;
  currency: string;
  deadlineMillis?: number;
  dateDeadline?: string;
  goalKind?: string;
  syncUpdatedAt: number;
  deleted?: boolean;
}

interface LegacySnapshotDebt {
  id: number | string;
  name: string;
  targetAmountCents: number;
  currentAmountCents: number;
  currency: string;
  status: string;
  dateGiven: number | string;
  dateDue?: number | string;
  syncUpdatedAt: number;
  deleted?: boolean;
}

interface LegacySnapshotDebtPayment {
  id: number | string;
  debtorId: number | string;
  amountCents: number;
  paidAt: number | string;
  syncUpdatedAt: number;
  deleted?: boolean;
}

interface LegacySnapshotTransfer {
  id: number | string;
  amount: number;
  date: number;
  isConfirmed: boolean;
  goalId?: number | string;
  syncUpdatedAt: number;
  deleted?: boolean;
}

interface LegacySnapshotExpense {
  id: number | string;
  amount: number;
  category: string;
  date: number;
  note?: string;
  isConfirmed: boolean;
  goalId?: number | string;
  syncUpdatedAt: number;
  deleted?: boolean;
}

const SYNC_UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function isSyncUuid(id: string): boolean {
  return SYNC_UUID.test(id);
}

function assignSyncId(id: number | string, assigned: Map<string, string>): string {
  const raw = String(id);
  const existing = assigned.get(raw);
  if (existing) return existing;
  const next = typeof id === "number" || /^\d+$/.test(raw) ? crypto.randomUUID() : raw;
  assigned.set(raw, next);
  return next;
}

function migrateLegacySnapshot(legacy: LegacyFinanceSnapshot): FinanceSnapshot {
  const assigned = new Map<string, string>();
  const transactions: SnapshotTransaction[] = legacy.transactions ?? [
    ...(legacy.transfers ?? []).map((transfer) => ({
      id: assignSyncId(transfer.id, assigned),
      kind: transfer.goalId == null ? ("DEPOSIT" as const) : ("GOAL_TRANSFER" as const),
      amountCents: Math.round(transfer.amount * 100),
      goalId: transfer.goalId == null ? undefined : assignSyncId(transfer.goalId, assigned),
      createdAt: new Date(transfer.date).toISOString(),
      syncUpdatedAt: transfer.syncUpdatedAt,
      deleted: transfer.deleted,
    })),
    ...(legacy.expenses ?? []).map((expense) => ({
      id: assignSyncId(expense.id, assigned),
      kind: "EXPENSE" as const,
      amountCents: Math.round(expense.amount * 100),
      createdAt: new Date(expense.date).toISOString(),
      category: expense.category,
      note: expense.note,
      ...(expense.goalId == null ? {} : { goalId: assignSyncId(expense.goalId, assigned) }),
      syncUpdatedAt: expense.syncUpdatedAt,
      deleted: expense.deleted,
    })),
  ];

  return {
    schemaVersion: SYNC_SCHEMA_VERSION,
    deviceId: legacy.deviceId,
    updatedAt: legacy.updatedAt,
    goals: legacy.goals.map((goal) => ({
      id: assignSyncId(goal.id, assigned),
      name: goal.name,
      targetAmountCents: goal.targetAmountCents,
      currency: goal.currency,
      ...(goal.goalKind ? { goalKind: goal.goalKind } : {}),
      dateDeadline: goal.dateDeadline
        ?? (goal.deadlineMillis != null
          ? new Date(goal.deadlineMillis).toISOString().slice(0, 10)
          : undefined),
      syncUpdatedAt: goal.syncUpdatedAt,
      deleted: goal.deleted,
    })),
    debts: legacy.debts.map((debt) => ({
      id: assignSyncId(debt.id, assigned),
      name: debt.name,
      targetAmountCents: debt.targetAmountCents,
      currentAmountCents: debt.currentAmountCents,
      currency: debt.currency,
      status: debt.status,
      dateGiven: typeof debt.dateGiven === "number"
        ? new Date(debt.dateGiven).toISOString().slice(0, 10)
        : debt.dateGiven,
      dateDue: debt.dateDue == null
        ? undefined
        : typeof debt.dateDue === "number"
          ? new Date(debt.dateDue).toISOString().slice(0, 10)
          : debt.dateDue,
      debtType: "CREDITOR",
      affectsBalance: false,
      note: "",
      syncUpdatedAt: debt.syncUpdatedAt,
      deleted: debt.deleted,
    })),
    debtPayments: legacy.debtPayments.map((payment) => ({
      id: assignSyncId(payment.id, assigned),
      debtorId: assignSyncId(payment.debtorId, assigned),
      amountCents: payment.amountCents,
      paidAt: typeof payment.paidAt === "number"
        ? new Date(payment.paidAt).toISOString()
        : payment.paidAt,
      affectsBalance: true,
      syncUpdatedAt: payment.syncUpdatedAt,
      deleted: payment.deleted,
    })),
    transactions,
    voiceSettings: legacy.voiceSettings,
  };
}

export function emptySnapshot(deviceId: string): FinanceSnapshot {
  return {
    schemaVersion: SYNC_SCHEMA_VERSION,
    deviceId,
    updatedAt: Date.now(),
    goals: [],
    debts: [],
    debtPayments: [],
    transactions: [],
  };
}

export type { SnapshotGoal, SnapshotDebt, SnapshotDebtPayment, SnapshotTransaction };

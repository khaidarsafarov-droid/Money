import { SYNC_SCHEMA_VERSION } from "./constants";
import type {
  FinanceSnapshot,
  SnapshotDebt,
  SnapshotDebtPayment,
  SnapshotGoal,
  SnapshotTransaction,
  SnapshotVoiceSettings,
} from "./sync";

export function encodeFinanceSnapshot(snapshot: FinanceSnapshot): string {
  return JSON.stringify(snapshot);
}

export function decodeFinanceSnapshot(raw: string): FinanceSnapshot {
  const json = JSON.parse(raw) as FinanceSnapshot;
  if (json.schemaVersion >= 2) {
    return json;
  }
  return migrateLegacySnapshot(json as LegacyFinanceSnapshot);
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
  syncUpdatedAt: number;
  deleted?: boolean;
}

function migrateLegacySnapshot(legacy: LegacyFinanceSnapshot): FinanceSnapshot {
  const transactions: SnapshotTransaction[] = legacy.transactions ?? [
    ...(legacy.transfers ?? []).map((transfer) => ({
      id: String(transfer.id),
      kind: transfer.goalId == null ? ("DEPOSIT" as const) : ("GOAL_TRANSFER" as const),
      amountCents: Math.round(transfer.amount * 100),
      goalId: transfer.goalId == null ? undefined : String(transfer.goalId),
      createdAt: new Date(transfer.date).toISOString(),
      syncUpdatedAt: transfer.syncUpdatedAt,
      deleted: transfer.deleted,
    })),
    ...(legacy.expenses ?? []).map((expense) => ({
      id: String(expense.id),
      kind: "EXPENSE" as const,
      amountCents: Math.round(expense.amount * 100),
      createdAt: new Date(expense.date).toISOString(),
      category: expense.category,
      note: expense.note,
      syncUpdatedAt: expense.syncUpdatedAt,
      deleted: expense.deleted,
    })),
  ];

  return {
    schemaVersion: SYNC_SCHEMA_VERSION,
    deviceId: legacy.deviceId,
    updatedAt: legacy.updatedAt,
    goals: legacy.goals.map((goal) => ({
      id: String(goal.id),
      name: goal.name,
      targetAmountCents: goal.targetAmountCents,
      currency: goal.currency,
      dateDeadline: goal.dateDeadline
        ?? (goal.deadlineMillis != null
          ? new Date(goal.deadlineMillis).toISOString().slice(0, 10)
          : undefined),
      syncUpdatedAt: goal.syncUpdatedAt,
      deleted: goal.deleted,
    })),
    debts: legacy.debts.map((debt) => ({
      id: String(debt.id),
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
      syncUpdatedAt: debt.syncUpdatedAt,
      deleted: debt.deleted,
    })),
    debtPayments: legacy.debtPayments.map((payment) => ({
      id: String(payment.id),
      debtorId: String(payment.debtorId),
      amountCents: payment.amountCents,
      paidAt: typeof payment.paidAt === "number"
        ? new Date(payment.paidAt).toISOString()
        : payment.paidAt,
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

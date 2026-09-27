import { SYNC_SCHEMA_VERSION } from "./constants";

export { SYNC_SCHEMA_VERSION };

export interface LwwRecord {
  syncUpdatedAt: number;
  deleted?: boolean;
}

export interface SnapshotGoal extends LwwRecord {
  id: string;
  name: string;
  targetAmountCents: number;
  currency: string;
  dateDeadline?: string;
}

export interface SnapshotDebt extends LwwRecord {
  id: string;
  name: string;
  targetAmountCents: number;
  currentAmountCents: number;
  currency: string;
  status: string;
  dateGiven: string;
  dateDue?: string;
  debtType: string;
  affectsBalance: boolean;
  note: string;
}

export interface SnapshotDebtPayment extends LwwRecord {
  id: string;
  debtorId: string;
  amountCents: number;
  paidAt: string;
  affectsBalance: boolean;
}

export interface SnapshotTransaction extends LwwRecord {
  id: string;
  kind: "DEPOSIT" | "GOAL_TRANSFER" | "EXPENSE";
  amountCents: number;
  goalId?: string;
  debtId?: string;
  debtPaymentId?: string;
  createdAt: string;
  note?: string;
  category?: string;
}

export interface SnapshotVoiceSettings {
  enabled: boolean;
  reportHour: number;
  reportMinute: number;
  language: string;
  gender: string;
  forceInSilentMode: boolean;
  lastVoiceReportDate?: string;
  lastAppOpenDate?: string;
  syncUpdatedAt: number;
}

/**
 * Canonical offline snapshot for P2P sync (Phase 1 contract).
 * Web uses `transactions[]`; Android Room uses `transfers[]` + `expenses[]` — see ARCHITECTURE.md.
 */
export interface FinanceSnapshot {
  schemaVersion: number;
  deviceId: string;
  updatedAt: number;
  goals: SnapshotGoal[];
  debts: SnapshotDebt[];
  debtPayments: SnapshotDebtPayment[];
  transactions: SnapshotTransaction[];
  voiceSettings?: SnapshotVoiceSettings;
}

export type SyncEntityType =
  | "GOAL"
  | "DEBT"
  | "DEBT_PAYMENT"
  | "TRANSACTION"
  | "VOICE_SETTINGS"
  | "FULL_SNAPSHOT";

export interface SyncQueueItem {
  id: string;
  entityType: SyncEntityType;
  entityId: string;
  payloadJson: string;
  createdAt: number;
}

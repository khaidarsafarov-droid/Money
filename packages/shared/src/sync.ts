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
  goalKind?: string;
  mode?: string;
}

export interface SnapshotGoalAction extends LwwRecord {
  id: string;
  goalId: string;
  title: string;
  completed: boolean;
  sortOrder: number;
}

export interface SnapshotNote extends LwwRecord {
  id: string;
  title: string;
  kind: string;
  body: string;
  mark: string;
  closed: boolean;
  createdAt: number;
  mode?: string;
}

export interface SnapshotClient extends LwwRecord {
  id: string;
  name: string;
  phone?: string;
  company?: string;
  email?: string;
  lastMeetingAt?: number;
  note?: string;
  createdAt: number;
  mode?: string;
}

export interface SnapshotNoteLine extends LwwRecord {
  id: string;
  noteId: string;
  label: string;
  amountCents: number;
  sortOrder: number;
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
  mode?: string;
  clientSyncId?: string;
}

export interface SnapshotDebtAdvance extends LwwRecord {
  id: string;
  debtorId: string;
  amountCents: number;
  givenAt: string;
  note?: string;
  currency?: string;
  mode?: string;
}

export interface SnapshotDebtPayment extends LwwRecord {
  id: string;
  debtorId: string;
  amountCents: number;
  paidAt: string;
  affectsBalance: boolean;
  currency?: string;
  mode?: string;
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
  mode?: string;
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
  goalActions?: SnapshotGoalAction[];
  notes?: SnapshotNote[];
  noteLines?: SnapshotNoteLine[];
  /** Absent on snapshots written before clients existed. */
  clients?: SnapshotClient[];
  /** Absent on snapshots written before disbursement history existed. */
  debtAdvances?: SnapshotDebtAdvance[];
}

export type SyncEntityType =
  | "GOAL"
  | "GOAL_ACTION"
  | "DEBT"
  | "DEBT_PAYMENT"
  | "DEBT_ADVANCE"
  | "TRANSACTION"
  | "NOTE"
  | "NOTE_LINE"
  | "CLIENT"
  | "VOICE_SETTINGS"
  | "FULL_SNAPSHOT";

export interface SyncQueueItem {
  id: string;
  entityType: SyncEntityType;
  entityId: string;
  payloadJson: string;
  createdAt: number;
}

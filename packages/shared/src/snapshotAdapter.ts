import { SYNC_SCHEMA_VERSION } from "./constants";
import type { FinanceState, FinancialItem, Payment, Transaction } from "./finance";
import type {
  FinanceSnapshot,
  SnapshotDebt,
  SnapshotDebtPayment,
  SnapshotGoal,
  SnapshotTransaction,
} from "./sync";

function debtStatusToFinancialStatus(status: string): FinancialItem["status"] {
  if (status === "COMPLETED") return "COMPLETED";
  if (status === "PARTIAL") return "PARTIAL";
  return "ACTIVE";
}

function financialStatusToDebtStatus(status: FinancialItem["status"]): string {
  return status;
}

export function financeStateToSnapshot(
  state: FinanceState,
  deviceId: string,
  updatedAt = Date.now(),
): FinanceSnapshot {
  const goals: SnapshotGoal[] = state.items
    .filter((item) => item.type === "GOAL")
    .map((goal) => ({
      id: goal.id,
      name: goal.name,
      targetAmountCents: goal.target,
      currency: state.currency,
      dateDeadline: goal.dateDeadline,
      syncUpdatedAt: goal.updatedAt ? Date.parse(goal.updatedAt) : updatedAt,
      deleted: goal.deleted,
    }));

  const debts: SnapshotDebt[] = state.items
    .filter((item) => item.type === "DEBT")
    .map((debt) => ({
      id: debt.id,
      name: debt.name,
      targetAmountCents: debt.target,
      currentAmountCents: debt.amount,
      currency: state.currency,
      status: financialStatusToDebtStatus(debt.status),
      dateGiven: debt.dateGiven ?? new Date().toISOString().slice(0, 10),
      dateDue: debt.dateDue,
      syncUpdatedAt: debt.updatedAt ? Date.parse(debt.updatedAt) : updatedAt,
      deleted: debt.deleted,
    }));

  const debtPayments: SnapshotDebtPayment[] = state.items
    .filter((item) => item.type === "DEBT")
    .flatMap((debt) =>
      debt.history.map((payment) => ({
        id: payment.id,
        debtorId: debt.id,
        amountCents: payment.amount,
        paidAt: payment.paidAt,
        syncUpdatedAt: Date.parse(payment.paidAt) || updatedAt,
      })),
    );

  const transactions: SnapshotTransaction[] = state.transactions.map((tx) => ({
    id: tx.id,
    kind: tx.kind,
    amountCents: tx.amount,
    goalId: tx.goalId,
    createdAt: tx.createdAt,
    note: tx.note,
    category: tx.category,
    syncUpdatedAt: Date.parse(tx.createdAt) || updatedAt,
  }));

  return {
    schemaVersion: SYNC_SCHEMA_VERSION,
    deviceId,
    updatedAt,
    goals,
    debts,
    debtPayments,
    transactions,
  };
}

export function snapshotToFinanceState(snapshot: FinanceSnapshot): FinanceState {
  const paymentsByDebtor = new Map<string, Payment[]>();
  for (const payment of snapshot.debtPayments) {
    if (payment.deleted) continue;
    const list = paymentsByDebtor.get(payment.debtorId) ?? [];
    list.push({
      id: payment.id,
      amount: payment.amountCents,
      paidAt: payment.paidAt,
    });
    paymentsByDebtor.set(payment.debtorId, list);
  }

  const goalItems: FinancialItem[] = snapshot.goals
    .filter((goal) => !goal.deleted)
    .map((goal) => ({
      id: goal.id,
      type: "GOAL",
      name: goal.name,
      status: "ACTIVE",
      amount: 0,
      target: goal.targetAmountCents,
      history: [],
      dateDeadline: goal.dateDeadline,
      updatedAt: new Date(goal.syncUpdatedAt).toISOString(),
    }));

  const debtItems: FinancialItem[] = snapshot.debts
    .filter((debt) => !debt.deleted)
    .map((debt) => ({
      id: debt.id,
      type: "DEBT",
      name: debt.name,
      status: debtStatusToFinancialStatus(debt.status),
      amount: debt.currentAmountCents,
      target: debt.targetAmountCents,
      history: paymentsByDebtor.get(debt.id) ?? [],
      dateGiven: debt.dateGiven,
      dateDue: debt.dateDue,
      updatedAt: new Date(debt.syncUpdatedAt).toISOString(),
    }));

  const transactions: Transaction[] = snapshot.transactions
    .filter((tx) => !tx.deleted)
    .map((tx) => ({
      id: tx.id,
      kind: tx.kind,
      amount: tx.amountCents,
      goalId: tx.goalId,
      createdAt: tx.createdAt,
      note: tx.note,
      category: tx.category,
    }));

  return {
    currency: "USD",
    items: [...goalItems, ...debtItems],
    transactions,
  };
}

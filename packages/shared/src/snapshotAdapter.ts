import { SYNC_SCHEMA_VERSION } from "./constants";
import type { DebtAdvance, FinanceState, FinancialItem, Payment, Transaction } from "./finance";
import type {
  FinanceSnapshot,
  SnapshotDebt,
  SnapshotDebtAdvance,
  SnapshotDebtPayment,
  SnapshotGoal,
  SnapshotTransaction,
} from "./sync";

function toInstant(value: string | undefined): string {
  if (!value) return new Date().toISOString();
  return value.includes("T") ? value : `${value}T00:00:00.000Z`;
}

function openingAdvance(debt: { id: string; target: number; dateGiven?: string; updatedAt?: string }): DebtAdvance {
  return {
    id: `open-${debt.id}`,
    amount: debt.target,
    givenAt: toInstant(debt.dateGiven ?? debt.updatedAt),
    note: "",
  };
}

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
      ...(goal.goalKind ? { goalKind: goal.goalKind } : {}),
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
      debtType: "CREDITOR",
      affectsBalance: false,
      note: "",
      syncUpdatedAt: debt.updatedAt ? Date.parse(debt.updatedAt) : updatedAt,
      deleted: debt.deleted,
    }));

  const debtAdvances: SnapshotDebtAdvance[] = state.items
    .filter((item) => item.type === "DEBT")
    .flatMap((debt) => {
      const advances = debt.advances?.filter((advance) => advance.amount > 0) ?? [];
      const source = advances.length > 0 ? advances : debt.target > 0 ? [openingAdvance(debt)] : [];
      return source.map((advance) => ({
        id: advance.id,
        debtorId: debt.id,
        amountCents: advance.amount,
        givenAt: toInstant(advance.givenAt),
        note: advance.note ?? "",
        syncUpdatedAt: advance.syncUpdatedAt ?? (Date.parse(toInstant(advance.givenAt)) || updatedAt),
        deleted: advance.deleted,
      }));
    });

  const debtPayments: SnapshotDebtPayment[] = state.items
    .filter((item) => item.type === "DEBT")
    .flatMap((debt) =>
      debt.history.map((payment) => ({
        id: payment.id,
        debtorId: debt.id,
        amountCents: payment.amount,
        paidAt: payment.paidAt,
        affectsBalance: payment.affectsBalance ?? true,
        ...(payment.currency ? { currency: payment.currency } : {}),
        syncUpdatedAt: payment.syncUpdatedAt ?? (Date.parse(payment.paidAt) || updatedAt),
        deleted: payment.deleted,
      })),
    );

  const transactions: SnapshotTransaction[] = state.transactions.map((tx) => ({
    id: tx.id,
    kind: tx.kind,
    amountCents: tx.amount,
    goalId: tx.goalId,
    debtId: tx.debtId,
    debtPaymentId: tx.debtPaymentId,
    createdAt: tx.createdAt,
    note: tx.note,
    category: tx.category,
    syncUpdatedAt: tx.syncUpdatedAt ?? (Date.parse(tx.createdAt) || updatedAt),
    deleted: tx.deleted,
  }));

  return {
    schemaVersion: SYNC_SCHEMA_VERSION,
    deviceId,
    updatedAt,
    goals,
    debts,
    debtPayments,
    transactions,
    debtAdvances,
  };
}

export function snapshotToFinanceState(snapshot: FinanceSnapshot): FinanceState {
  const advancesByDebtor = new Map<string, DebtAdvance[]>();
  for (const advance of snapshot.debtAdvances ?? []) {
    const list = advancesByDebtor.get(advance.debtorId) ?? [];
    list.push({
      id: advance.id,
      amount: advance.amountCents,
      givenAt: advance.givenAt,
      note: advance.note,
      syncUpdatedAt: advance.syncUpdatedAt,
      deleted: advance.deleted,
    });
    advancesByDebtor.set(advance.debtorId, list);
  }

  const paymentsByDebtor = new Map<string, Payment[]>();
  for (const payment of snapshot.debtPayments) {
    const list = paymentsByDebtor.get(payment.debtorId) ?? [];
    list.push({
      id: payment.id,
      amount: payment.amountCents,
      paidAt: payment.paidAt,
      affectsBalance: payment.affectsBalance,
      ...(payment.currency ? { currency: payment.currency } : {}),
      syncUpdatedAt: payment.syncUpdatedAt,
      deleted: payment.deleted,
    });
    paymentsByDebtor.set(payment.debtorId, list);
  }

  const goalItems: FinancialItem[] = snapshot.goals
    .map((goal) => ({
      id: goal.id,
      type: "GOAL",
      name: goal.name,
      status: "ACTIVE",
      amount: 0,
      target: goal.targetAmountCents,
      history: [],
      dateDeadline: goal.dateDeadline,
      ...(goal.goalKind ? { goalKind: goal.goalKind } : {}),
      updatedAt: new Date(goal.syncUpdatedAt).toISOString(),
      deleted: goal.deleted,
    }));

  const debtItems: FinancialItem[] = snapshot.debts
    .map((debt) => ({
      id: debt.id,
      type: "DEBT",
      name: debt.name,
      status: debtStatusToFinancialStatus(debt.status),
      amount: debt.currentAmountCents,
      target: debt.targetAmountCents,
      history: paymentsByDebtor.get(debt.id) ?? [],
      advances: (advancesByDebtor.get(debt.id) ?? []).filter((advance) => !advance.deleted).length > 0
        ? advancesByDebtor.get(debt.id)
        : debt.targetAmountCents > 0
          ? [openingAdvance({
              id: debt.id,
              target: debt.targetAmountCents,
              dateGiven: debt.dateGiven,
              updatedAt: new Date(debt.syncUpdatedAt).toISOString(),
            })]
          : [],
      dateGiven: debt.dateGiven,
      dateDue: debt.dateDue,
      updatedAt: new Date(debt.syncUpdatedAt).toISOString(),
      deleted: debt.deleted,
    }));

  const transactions: Transaction[] = snapshot.transactions
    .map((tx) => ({
      id: tx.id,
      kind: tx.kind,
      amount: tx.amountCents,
      goalId: tx.goalId,
      debtId: tx.debtId,
      debtPaymentId: tx.debtPaymentId,
      createdAt: tx.createdAt,
      note: tx.note,
      category: tx.category,
      syncUpdatedAt: tx.syncUpdatedAt,
      deleted: tx.deleted,
    }));

  return {
    currency: "USD",
    items: [...goalItems, ...debtItems],
    transactions,
  };
}

import {
  addDebt,
  addGoal,
  computeDashboardTotals,
  filterByType,
  filterItems,
  isDebtOverdue,
  processPayment,
  removeItem,
  sortItems,
} from "@/lib/financeEngine";
import { enrichFinanceState, migrateLegacyGoalHistory } from "@/lib/globalFinanceEngine";
import { createId } from "@/lib/money";
import type { FinanceState, FinancialItem, FinancialStatus } from "@/types/finance";

const STORAGE_KEY = "truckerload-finance-v5";

interface LegacyDebt {
  id: string;
  name: string;
  targetValueCents: number;
  currentValueCents: number;
  status: FinancialStatus;
  dateGiven: string;
  dateDue: string;
  paymentHistory: { id: string; amountCents: number; paidAt: string }[];
}

interface LegacyGoal {
  id: string;
  name: string;
  targetValueCents: number;
  currentValueCents: number;
  updatedAt: string;
}

interface LegacyStateV3 {
  goals: LegacyGoal[];
  debts: LegacyDebt[];
}

function createInitialState(): FinanceState {
  return {
    currency: "USD",
    items: [
      {
        id: createId(),
        type: "GOAL",
        name: "Резерв на рейс",
        status: "ACTIVE",
        amount: 0,
        target: 1_000_000,
        history: [],
        updatedAt: new Date().toISOString(),
      },
      {
        id: createId(),
        type: "DEBT",
        name: "Алексей",
        status: "ACTIVE",
        amount: 0,
        target: 50_000,
        history: [],
        dateGiven: "2026-03-01",
        dateDue: "2026-06-01",
        updatedAt: new Date().toISOString(),
      },
    ],
    transactions: [],
  };
}

function migrateFromV3(raw: LegacyStateV3): FinanceState {
  const debtItems: FinancialItem[] = raw.debts.map((debt) => ({
    id: debt.id,
    type: "DEBT",
    name: debt.name,
    status: debt.status,
    amount: debt.currentValueCents,
    target: debt.targetValueCents,
    history: debt.paymentHistory.map((p) => ({
      id: p.id,
      amount: p.amountCents,
      paidAt: p.paidAt,
    })),
    dateGiven: debt.dateGiven,
    dateDue: debt.dateDue,
    updatedAt: new Date().toISOString(),
  }));

  const goalItems: FinancialItem[] = raw.goals.map((goal) => {
    const amount = goal.currentValueCents;
    const target = goal.targetValueCents;
    let status: FinancialStatus = "ACTIVE";
    if (amount >= target && target > 0) status = "COMPLETED";
    else if (amount > 0) status = "PARTIAL";

    return {
      id: goal.id,
      type: "GOAL",
      name: goal.name,
      status,
      amount,
      target,
      history: [],
      updatedAt: goal.updatedAt,
    };
  });

  return migrateLegacyGoalHistory({
    currency: "USD",
    items: [...goalItems, ...debtItems],
    transactions: [],
  });
}

function normalizeState(parsed: FinanceState): FinanceState {
  const withTransactions: FinanceState = {
    currency: "USD",
    items: parsed.items,
    transactions: parsed.transactions ?? [],
  };
  return enrichFinanceState(migrateLegacyGoalHistory(withTransactions));
}

function parseStoredState(raw: string): FinanceState | null {
  try {
    const parsed = JSON.parse(raw) as FinanceState | LegacyStateV3;
    if ("items" in parsed && Array.isArray(parsed.items)) {
      return normalizeState(parsed as FinanceState);
    }
    if ("debts" in parsed && "goals" in parsed) {
      return migrateFromV3(parsed as LegacyStateV3);
    }
    return null;
  } catch {
    return null;
  }
}

export function loadFinanceState(): FinanceState {
  if (typeof window === "undefined") return createInitialState();

  const keys = [
    STORAGE_KEY,
    "truckerload-finance-v4",
    "truckerload-finance-v3",
    "truckerload-finance-v2",
  ];
  for (const key of keys) {
    const raw = window.localStorage.getItem(key);
    if (!raw) continue;
    const state = parseStoredState(raw);
    if (state) {
      if (key !== STORAGE_KEY) saveFinanceState(state);
      return state;
    }
  }
  return createInitialState();
}

export function saveFinanceState(state: FinanceState): void {
  window.localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      currency: "USD",
      items: state.items,
      transactions: state.transactions,
    }),
  );
}

export {
  addDebt,
  addGoal,
  computeDashboardTotals,
  filterByType,
  filterItems,
  isDebtOverdue,
  processPayment,
  removeItem,
  sortItems,
};

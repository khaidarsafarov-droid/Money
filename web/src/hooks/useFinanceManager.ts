import { useCallback, useMemo, useState } from "react";
import {
  addDebt as addDebtToState,
  addGoal as addGoalToState,
  computeDashboardTotals,
  filterByType,
  filterItems,
  loadFinanceState,
  processPayment as applyPayment,
  removeItem,
  saveFinanceState,
  sortItems,
} from "@/lib/financeStore";
import {
  addTransaction,
  computeGlobalFinanceSummary,
  enrichFinanceState,
  sortedTransactions,
} from "@/lib/globalFinanceEngine";
import { formatUsd, parseMoneyInput } from "@/lib/money";
import type { FinanceState, FinancialItem, StatusFilter, TypeFilter, Transaction } from "@/types/finance";

export function useFinanceManager() {
  const [state, setState] = useState<FinanceState>(() => loadFinanceState());
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("active");
  const [typeFilter, setTypeFilter] = useState<TypeFilter>("all");

  const enrichedState = useMemo(() => enrichFinanceState(state), [state]);

  const persist = useCallback((next: FinanceState) => {
    setState(next);
    saveFinanceState(next);
  }, []);

  const replaceState = useCallback(
    (next: FinanceState) => {
      persist(next);
    },
    [persist],
  );

  const globalFinance = useMemo(
    () => computeGlobalFinanceSummary(enrichedState),
    [enrichedState],
  );

  const dashboardTotals = useMemo(
    () => computeDashboardTotals(enrichedState),
    [enrichedState],
  );

  const visibleItems = useMemo(
    () =>
      sortItems(
        filterByType(filterItems(enrichedState.items, statusFilter), typeFilter),
      ),
    [enrichedState.items, statusFilter, typeFilter],
  );

  const transactions = useMemo(
    () => sortedTransactions(enrichedState.transactions),
    [enrichedState.transactions],
  );

  const processPayment = useCallback(
    (id: string, amountInput: string): { ok: boolean; error?: string } => {
      const paymentCents = parseMoneyInput(amountInput);
      if (!paymentCents) {
        return { ok: false, error: "Введите корректную сумму" };
      }
      persist(applyPayment(enrichedState, id, paymentCents));
      return { ok: true };
    },
    [persist, enrichedState],
  );

  const addGoalTransfer = useCallback(
    (goalId: string, amountInput: string): { ok: boolean; error?: string } => {
      const amountCents = parseMoneyInput(amountInput);
      if (!amountCents) {
        return { ok: false, error: "Введите корректную сумму" };
      }
      const result = addTransaction(enrichedState, {
        kind: "GOAL_TRANSFER",
        amountCents,
        goalId,
      });
      if (result.error) return { ok: false, error: result.error };
      persist(result.state);
      return { ok: true };
    },
    [persist, enrichedState],
  );

  const addDeposit = useCallback(
    (amountInput: string): { ok: boolean; error?: string } => {
      const amountCents = parseMoneyInput(amountInput);
      if (!amountCents) {
        return { ok: false, error: "Введите корректную сумму" };
      }
      const result = addTransaction(enrichedState, {
        kind: "DEPOSIT",
        amountCents,
      });
      if (result.error) return { ok: false, error: result.error };
      persist(result.state);
      return { ok: true };
    },
    [persist, enrichedState],
  );

  const addExpense = useCallback(
    (amountInput: string, category = "Общий", goalId?: string): { ok: boolean; error?: string } => {
      const amountCents = parseMoneyInput(amountInput);
      if (!amountCents) {
        return { ok: false, error: "Введите корректную сумму" };
      }
      const result = addTransaction(enrichedState, {
        kind: "EXPENSE",
        amountCents,
        category,
        goalId,
      });
      if (result.error) return { ok: false, error: result.error };
      persist(result.state);
      return { ok: true };
    },
    [persist, enrichedState],
  );

  const addGoal = useCallback(
    (input: { name: string; target: number; dateDeadline?: string }) => {
      persist(addGoalToState(enrichedState, input));
    },
    [persist, enrichedState],
  );

  const addDebt = useCallback(
    (input: {
      name: string;
      target: number;
      dateGiven: string;
      dateDue: string;
    }) => {
      persist(addDebtToState(enrichedState, input));
    },
    [persist, enrichedState],
  );

  const removeFinancialItem = useCallback(
    (id: string) => {
      persist(removeItem(enrichedState, id));
    },
    [persist, enrichedState],
  );

  return {
    state: enrichedState,
    statusFilter,
    setStatusFilter,
    typeFilter,
    setTypeFilter,
    visibleItems,
    transactions,
    globalFinance,
    dashboardTotals,
    processPayment,
    addGoalTransfer,
    addDeposit,
    addExpense,
    addGoal,
    addDebt,
    removeItem: removeFinancialItem,
    replaceState,
    formatUsd,
  };
}

export type FinanceManager = ReturnType<typeof useFinanceManager>;
export type { FinancialItem, StatusFilter, Transaction, TypeFilter };

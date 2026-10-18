import { isGoalDeadlinePassed } from "@/lib/goalProgress";
import { createId } from "@/lib/money";
import type { DashboardTotals, DebtAdvance, FinanceState, FinancialItem, FinancialStatus, Payment, StatusFilter, TypeFilter } from "@/types/finance";
import { debtRemainingCents } from "@/types/finance";
import { computeGlobalFinanceSummary, enrichFinanceState, addTransaction } from "@/lib/globalFinanceEngine";

export function resolveStatus(target: number, amount: number): FinancialStatus {
  if (amount <= 0) return "ACTIVE";
  if (amount >= target && target > 0) return "COMPLETED";
  return "PARTIAL";
}

export function remainingAmount(item: FinancialItem): number {
  return Math.max(item.target - item.amount, 0);
}

export function progressFraction(item: FinancialItem): number {
  if (item.target <= 0) return 0;
  return Math.min(item.amount / item.target, 1);
}

export function activeItems(items: FinancialItem[]): FinancialItem[] {
  return items.filter((item) => !item.deleted);
}

export function filterItems(
  items: FinancialItem[],
  filter: StatusFilter,
): FinancialItem[] {
  const visible = activeItems(items);
  switch (filter) {
    case "active":
      return visible.filter((item) => item.status !== "COMPLETED");
    case "completed":
      return visible.filter((item) => item.status === "COMPLETED");
    default:
      return visible;
  }
}

export function filterByType(
  items: FinancialItem[],
  filter: TypeFilter,
): FinancialItem[] {
  const visible = activeItems(items);
  switch (filter) {
    case "goal":
      return visible.filter((item) => item.type === "GOAL");
    case "debt":
      return visible.filter((item) => item.type === "DEBT");
    default:
      return visible;
  }
}

export function isDebtOverdue(item: FinancialItem): boolean {
  if (item.type !== "DEBT" || item.status === "COMPLETED" || !item.dateDue) {
    return false;
  }
  const due = new Date(item.dateDue);
  due.setHours(23, 59, 59, 999);
  return due.getTime() < Date.now();
}

function itemSortRank(item: FinancialItem): number {
  if (item.type === "DEBT") {
    if (item.status === "COMPLETED") return 3;
    if (!item.dateDue) return 2;
    if (isDebtOverdue(item)) return 0;
    return 1;
  }
  if (
    item.status !== "COMPLETED" &&
    item.dateDeadline &&
    isGoalDeadlinePassed(item.dateDeadline)
  ) {
    return 0;
  }
  if (item.status === "COMPLETED") return 2;
  return 1;
}

export function sortItems(items: FinancialItem[]): FinancialItem[] {
  return [...items].sort((a, b) => {
    const rankDiff = itemSortRank(a) - itemSortRank(b);
    if (rankDiff !== 0) return rankDiff;
    if (a.type === "GOAL" && b.type === "GOAL") {
      const deadlineDiff = (a.dateDeadline ?? "9999-99-99").localeCompare(b.dateDeadline ?? "9999-99-99");
      if (deadlineDiff !== 0) return deadlineDiff;
    }
    if (a.type === "DEBT" && b.type === "DEBT" && a.dateDue && b.dateDue) {
      return a.dateDue.localeCompare(b.dateDue);
    }
    return (b.updatedAt ?? "").localeCompare(a.updatedAt ?? "");
  });
}

/**
 * Home ring: paid «I owe» amounts plus goal savings, over those debt targets
 * plus the goal pool. An empty pool is 0. Truncates toward zero so the
 * percent matches the Android engine.
 */
export function planProgressPercent(input: {
  debtPaidCents: number;
  debtTargetCents: number;
  goalAccumulatedCents: number;
  goalRemainingCents: number;
}): number {
  const paid = input.debtPaidCents + input.goalAccumulatedCents;
  const pool = input.debtTargetCents + input.goalAccumulatedCents + input.goalRemainingCents;
  if (pool <= 0) return 0;
  return Math.min(100, Math.max(0, Math.trunc((paid / pool) * 100)));
}

export function computeDashboardTotals(state: FinanceState): DashboardTotals {
  const enriched = enrichFinanceState(state);
  const visibleItems = enriched.items.filter((item) => !item.deleted);
  const activeItems = visibleItems.filter((item) => item.status !== "COMPLETED");
  const completedItems = visibleItems.filter((item) => item.status === "COMPLETED");

  const debts = visibleItems.filter((item) => item.type === "DEBT");
  const finance = computeGlobalFinanceSummary(enriched);

  const totalSavedUsd = finance.totalAccumulatedCents / 100;
  const totalDebtRemainingUsd = debts
    .filter((d) => d.status !== "COMPLETED")
    .reduce((sum, d) => sum + remainingAmount(d) / 100, 0);

  return {
    globalBalanceUsd: finance.globalBalanceCents / 100,
    totalSavedUsd,
    totalRemainingToGoalUsd: finance.totalRemainingToGoalCents / 100,
    totalDebtRemainingUsd,
    availableBalanceUsd: finance.availableBalanceCents / 100,
    activeCount: activeItems.length,
    completedCount: completedItems.length,
  };
}

export function processPayment(
  state: FinanceState,
  id: string,
  paymentCents: number,
  paidAt?: string,
): FinanceState {
  if (paymentCents <= 0) return state;

  const item = state.items.find((entry) => entry.id === id);
  if (!item) return state;

  if (item.type === "GOAL") {
    const result = addTransaction(state, {
      kind: "GOAL_TRANSFER",
      amountCents: paymentCents,
      goalId: id,
    });
    return result.error ? state : result.state;
  }

  const items = state.items.map((entry) => {
    if (entry.id !== id) return entry;

    const capped = Math.min(paymentCents, remainingAmount(entry));
    if (capped <= 0) return entry;

    const newAmount = entry.amount + capped;
    const payment: Payment = {
      id: createId(),
      amount: capped,
      paidAt: paidAt
        ? paidAt.includes("T")
          ? paidAt
          : `${paidAt}T00:00:00.000Z`
        : new Date().toISOString(),
      syncUpdatedAt: Date.now(),
    };

    return {
      ...entry,
      amount: newAmount,
      status: resolveStatus(entry.target, newAmount),
      history: [payment, ...entry.history],
      updatedAt: new Date().toISOString(),
    };
  });

  return { ...state, items };
}

export function addGoal(
  state: FinanceState,
  input: { name: string; target: number; dateDeadline?: string },
): FinanceState {
  const item: FinancialItem = {
    id: createId(),
    type: "GOAL",
    name: input.name,
    target: input.target,
    amount: 0,
    status: "ACTIVE",
    history: [],
    dateDeadline: input.dateDeadline,
    updatedAt: new Date().toISOString(),
  };
  return enrichFinanceState({ ...state, items: [item, ...state.items] });
}

export function openingDebtAdvance(debt: {
  id: string;
  target: number;
  dateGiven?: string;
}): DebtAdvance {
  const givenAt = debt.dateGiven?.includes("T")
    ? debt.dateGiven
    : `${debt.dateGiven ?? new Date().toISOString().slice(0, 10)}T00:00:00.000Z`;
  return {
    id: `open-${debt.id}`,
    amount: debt.target,
    givenAt,
    note: "",
  };
}

export function withOpeningAdvance(item: FinancialItem): FinancialItem {
  if (item.type !== "DEBT" || item.deleted) return item;
  const live = item.advances?.filter((advance) => !advance.deleted && advance.amount > 0) ?? [];
  if (live.length > 0) return { ...item, advances: item.advances };
  if (item.target <= 0) return { ...item, advances: item.advances ?? [] };
  return { ...item, advances: [openingDebtAdvance(item)] };
}

export function addDebt(
  state: FinanceState,
  input: { name: string; target: number; dateGiven: string; dateDue: string },
): FinanceState {
  const id = createId();
  const item: FinancialItem = {
    id,
    type: "DEBT",
    name: input.name,
    target: input.target,
    amount: 0,
    status: "ACTIVE",
    history: [],
    advances: [openingDebtAdvance({ id, target: input.target, dateGiven: input.dateGiven })],
    dateGiven: input.dateGiven,
    dateDue: input.dateDue,
    updatedAt: new Date().toISOString(),
  };
  return { ...state, items: [item, ...state.items] };
}

export function addDebtAdvance(
  state: FinanceState,
  debtId: string,
  amountCents: number,
  givenAt: string,
  note = "",
): FinanceState {
  if (amountCents <= 0) return state;
  return {
    ...state,
    items: state.items.map((item) => {
      if (item.id !== debtId || item.type !== "DEBT" || item.deleted) return item;
      const seeded = withOpeningAdvance(item);
      const advance: DebtAdvance = {
        id: createId(),
        amount: amountCents,
        givenAt: givenAt.includes("T") ? givenAt : `${givenAt}T00:00:00.000Z`,
        note,
        syncUpdatedAt: Date.now(),
      };
      const target = item.target + amountCents;
      return {
        ...seeded,
        target,
        advances: [...(seeded.advances ?? []), advance],
        status: resolveStatus(target, item.amount),
        updatedAt: new Date().toISOString(),
      };
    }),
  };
}

export { debtRemainingCents };

export function removeItem(state: FinanceState, id: string): FinanceState {
  const now = new Date().toISOString();
  return {
    ...state,
    items: state.items.map((item) =>
      item.id === id ? { ...item, deleted: true, updatedAt: now } : item,
    ),
  };
}

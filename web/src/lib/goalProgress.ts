import type { FinancialItem } from "@/types/finance";
import { remainingAmount } from "@/lib/financeEngine";

const DAY_MS = 86_400_000;

function startOfDayMs(date: Date): number {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  return d.getTime();
}

function endOfDayMs(date: Date): number {
  const d = new Date(date);
  d.setHours(23, 59, 59, 999);
  return d.getTime();
}

export function progressPercentRaw(item: FinancialItem): number {
  if (item.target <= 0) return 0;
  return Math.round((item.amount / item.target) * 100);
}

export function displayProgressFraction(item: FinancialItem): number {
  return Math.min(progressPercentRaw(item) / 100, 1);
}

export function isGoalOverAchieved(item: FinancialItem): boolean {
  return item.type === "GOAL" && item.target > 0 && item.amount > item.target;
}

export function goalSurplusCents(item: FinancialItem): number {
  if (!isGoalOverAchieved(item)) return 0;
  return item.amount - item.target;
}

export function daysUntilDeadline(dateDeadline: string, now = Date.now()): number | null {
  const end = endOfDayMs(new Date(dateDeadline));
  const diff = end - startOfDayMs(new Date(now));
  if (diff < 0) return null;
  return Math.max(1, Math.ceil(diff / DAY_MS));
}

export function isGoalDeadlinePassed(dateDeadline?: string, now = Date.now()): boolean {
  if (!dateDeadline) return false;
  return endOfDayMs(new Date(dateDeadline)) < now;
}

export function dailyGoalCents(item: FinancialItem, now = Date.now()): number | null {
  if (item.type !== "GOAL" || !item.dateDeadline) return null;
  const remaining = remainingAmount(item);
  if (remaining <= 0) return null;
  const days = daysUntilDeadline(item.dateDeadline, now);
  if (days == null) return null;
  return Math.ceil(remaining / days);
}

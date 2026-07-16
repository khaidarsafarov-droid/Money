import type { FinancialItem } from "@/types/finance";
import { remainingAmount } from "@/lib/financeEngine";

const DAY_MS = 86_400_000;

function parseDateOnlyUtc(dateStr: string): Date {
  const [year, month, day] = dateStr.split("-").map(Number);
  return new Date(Date.UTC(year, month - 1, day));
}

function startOfDayMs(date: Date): number {
  return Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate());
}

function endOfDayMs(date: Date): number {
  return Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate(), 23, 59, 59, 999);
}

function toUtcDate(input: string | number | Date): Date {
  if (typeof input === "string" && /^\d{4}-\d{2}-\d{2}$/.test(input)) {
    return parseDateOnlyUtc(input);
  }
  return new Date(input);
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
  const endDay = startOfDayMs(toUtcDate(dateDeadline));
  const nowDay = startOfDayMs(toUtcDate(now));
  const diffDays = Math.round((endDay - nowDay) / DAY_MS);
  if (diffDays < 0) return null;
  return Math.max(1, diffDays);
}

export function isGoalDeadlinePassed(dateDeadline?: string, now = Date.now()): boolean {
  if (!dateDeadline) return false;
  return endOfDayMs(toUtcDate(dateDeadline)) < now;
}

export function dailyGoalCents(item: FinancialItem, now = Date.now()): number | null {
  if (item.type !== "GOAL" || !item.dateDeadline) return null;
  const remaining = remainingAmount(item);
  if (remaining <= 0) return null;
  const days = daysUntilDeadline(item.dateDeadline, now);
  if (days == null) return null;
  return Math.ceil(remaining / days);
}

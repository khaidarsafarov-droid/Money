import type { CurrencyCode, FinancialItem } from "@/types/finance";
import { remainingAmount } from "@/lib/financeEngine";

export const APP_CURRENCY: CurrencyCode = "USD";
export const MASKED_AMOUNT = "•••";

export function toCents(amount: number): number {
  return Math.round(amount * 100);
}

export function fromCents(cents: number): number {
  return cents / 100;
}

export function parseMoneyInput(raw: string): number | null {
  const normalized = raw.trim().replace(",", ".");
  if (!normalized) return null;
  const value = Number.parseFloat(normalized);
  if (!Number.isFinite(value) || value <= 0) return null;
  return toCents(value);
}

/** Russian UI, currency code from shared (`USD`) until the web has a region picker. */
const MONEY_LOCALE = "ru";

export function formatMoney(cents: number, options?: { hidden?: boolean }): string {
  if (options?.hidden) return MASKED_AMOUNT;
  return new Intl.NumberFormat(MONEY_LOCALE, {
    style: "currency",
    currency: APP_CURRENCY,
    maximumFractionDigits: 2,
  }).format(fromCents(cents));
}

export function formatUsd(amount: number, options?: { hidden?: boolean }): string {
  return formatMoney(toCents(amount), options);
}

export function createId(): string {
  return crypto.randomUUID();
}

/** @deprecated Use remainingAmount from financeEngine */
export function remainingCents(item: FinancialItem): number {
  return remainingAmount(item);
}

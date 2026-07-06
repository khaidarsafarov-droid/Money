import { useEffect } from "react";
import type { FinancialItem } from "@/types/finance";
import { resolvePaymentItem } from "@/lib/paymentDeepLink";

const PAY_PARAM = "pay";

export function readPaymentDeepLinkId(): string | null {
  if (typeof window === "undefined") return null;
  return new URLSearchParams(window.location.search).get(PAY_PARAM);
}

export function clearPaymentDeepLink(): void {
  if (typeof window === "undefined") return;
  const url = new URL(window.location.href);
  url.searchParams.delete(PAY_PARAM);
  window.history.replaceState({}, "", url.pathname + url.search + url.hash);
}

export function buildPaymentDeepLink(id: string): string {
  if (typeof window === "undefined") return `?${PAY_PARAM}=${id}`;
  const url = new URL(window.location.href);
  url.searchParams.set(PAY_PARAM, id);
  return url.toString();
}

/** Opens the payment modal when ?pay=<itemId> is present (widget / external deep link). */
export function usePaymentDeepLink(
  items: FinancialItem[],
  onOpenPayment: (item: FinancialItem) => void,
): void {
  useEffect(() => {
    const payId = readPaymentDeepLinkId();
    if (!payId) return;

    const item = resolvePaymentItem(items, payId);
    if (item && item.status !== "COMPLETED") {
      onOpenPayment(item);
    }
    clearPaymentDeepLink();
  }, [items, onOpenPayment]);
}

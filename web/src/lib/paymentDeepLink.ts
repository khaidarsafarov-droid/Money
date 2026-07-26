import type { FinancialItem } from "@/types/finance";
import { itemPayKey } from "@/lib/debtRemind";

/** Resolves ?pay= deep link id to a financial item (UUID, payKey, or debt-/goal- prefix). */
export function resolvePaymentItem(
  items: FinancialItem[],
  payId: string,
): FinancialItem | undefined {
  const activeItems = items.filter((item) => !item.deleted);
  const direct = activeItems.find((item) => item.id === payId);
  if (direct) return direct;

  const byPayKey = activeItems.find((item) => itemPayKey(item) === payId);
  if (byPayKey) return byPayKey;

  const prefixed = activeItems.find((item) => itemPayKey(item) === payId.trim());
  return prefixed;
}

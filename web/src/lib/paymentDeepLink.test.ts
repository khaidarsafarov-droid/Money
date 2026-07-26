import { describe, expect, it } from "vitest";
import { addDebt } from "@/lib/financeEngine";
import { resolvePaymentItem } from "@/lib/paymentDeepLink";
import { itemPayKey } from "@/lib/debtRemind";
import type { FinanceState } from "@/types/finance";

const emptyState: FinanceState = { currency: "USD", items: [], transactions: [] };

describe("resolvePaymentItem", () => {
  const state = addDebt(emptyState, {
    name: "Alex",
    target: 50_000,
    dateGiven: "2026-01-01",
    dateDue: "2026-02-01",
  });
  const item = state.items[0];

  it("matches raw item id", () => {
    expect(resolvePaymentItem(state.items, item.id)?.id).toBe(item.id);
  });

  it("matches payKey prefix", () => {
    expect(resolvePaymentItem(state.items, itemPayKey(item))?.id).toBe(item.id);
  });

  it("returns undefined for unknown id", () => {
    expect(resolvePaymentItem(state.items, "debt-999")).toBeUndefined();
  });

  it("does not resolve sync tombstones", () => {
    expect(
      resolvePaymentItem([{ ...item, deleted: true }], item.id),
    ).toBeUndefined();
  });
});

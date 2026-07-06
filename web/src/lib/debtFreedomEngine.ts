import type { FinancialItem } from "@/types/finance";
import {
  calculateFreedomProjection,
  computeDebtMilestones,
  estimatePaymentsToClose,
  isSmallDebt,
  suggestAcceleratePaymentCents,
  type DebtFreedomDebtInput,
  type FreedomProjection,
} from "@money/shared";

export {
  calculateFreedomProjection,
  computeDebtMilestones,
  estimatePaymentsToClose,
  isSmallDebt,
  suggestAcceleratePaymentCents,
  type DebtFreedomDebtInput,
  type FreedomProjection,
};

export function toDebtFreedomInput(item: FinancialItem): DebtFreedomDebtInput {
  return {
    remainingCents: Math.max(item.target - item.amount, 0),
    targetCents: item.target,
    currentCents: item.amount,
    isCompleted: item.status === "COMPLETED",
    payments: item.history.map((payment) => ({
      amountCents: payment.amount,
      paidAtMs: new Date(payment.paidAt).getTime(),
    })),
    dateDueMs: item.dateDue ? new Date(item.dateDue).getTime() : undefined,
  };
}

export function projectDebtFreedom(
  items: FinancialItem[],
  extraMonthlyCents = 0,
): FreedomProjection {
  const debts = items
    .filter((item) => item.type === "DEBT")
    .map(toDebtFreedomInput);
  return calculateFreedomProjection(debts, extraMonthlyCents);
}

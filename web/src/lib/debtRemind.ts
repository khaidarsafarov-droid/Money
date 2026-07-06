import type { FinancialItem } from "@/types/finance";
import { isDebtOverdue, remainingAmount } from "@/lib/financeEngine";
import { formatMoney } from "@/lib/money";

export function itemPayKey(item: FinancialItem): string {
  const prefix = item.type === "DEBT" ? "debt" : "goal";
  return `${prefix}-${item.id}`;
}

export function buildDebtRemindMessage(item: FinancialItem): string {
  const remaining = formatMoney(remainingAmount(item));
  const dueDate = item.dateDue
    ? new Date(item.dateDue).toLocaleDateString("ru-RU")
    : "не указан";
  const overdue = isDebtOverdue(item);

  if (overdue) {
    return `Здравствуйте, ${item.name}! Долг ${remaining} просрочен (срок был ${dueDate}). Пожалуйста, свяжитесь для уточнения возврата.`;
  }

  return `Здравствуйте, ${item.name}! Напоминаю о возврате ${remaining}. Срок: ${dueDate}. Спасибо!`;
}

export async function shareDebtReminder(item: FinancialItem): Promise<void> {
  const message = buildDebtRemindMessage(item);
  const payload = {
    title: `Напоминание о долге — ${item.name}`,
    text: message,
  };

  if (typeof navigator !== "undefined" && navigator.share) {
    await navigator.share(payload);
    return;
  }

  await navigator.clipboard.writeText(message);
}

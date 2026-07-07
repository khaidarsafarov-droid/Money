export type DebtCalendarDebtType = "CREDITOR" | "DEBTOR";

export interface DebtCalendarEventInput {
  id: string;
  name: string;
  debtType: DebtCalendarDebtType;
  remainingCents: number;
  targetCents: number;
  dateGivenMs: number;
  dateDueMs: number;
  interestRateBps?: number;
  loanCategoryLabel?: string;
  isCompleted?: boolean;
  displayName?: string;
}

export interface CalendarReminderSettings {
  remindOneDayBefore: boolean;
  remindThreeDaysBefore: boolean;
  remindOnDueDay: boolean;
}

export const DEFAULT_CALENDAR_REMINDERS: CalendarReminderSettings = {
  remindOneDayBefore: true,
  remindThreeDaysBefore: false,
  remindOnDueDay: true,
};

export function formatDebtCalendarAmount(cents: number): string {
  return `$${(cents / 100).toFixed(2)}`;
}

export function buildDebtCalendarTitle(input: DebtCalendarEventInput): string {
  const amount = formatDebtCalendarAmount(input.remainingCents);
  const prefix = input.isCompleted ? "[ЗАКРЫТ] " : "";
  if (input.debtType === "CREDITOR") {
    return `${prefix}${input.name} должен(на) мне ${amount}`;
  }
  return `${prefix}Я должен(на) ${input.name} ${amount}`;
}

export function buildDebtCalendarDescription(input: DebtCalendarEventInput): string {
  const lines = [
    `Сумма: ${formatDebtCalendarAmount(input.remainingCents)}`,
    `Изначально: ${formatDebtCalendarAmount(input.targetCents)}`,
    `Дата выдачи: ${formatDateLabel(input.dateGivenMs)}`,
  ];
  if (input.interestRateBps && input.interestRateBps > 0) {
    lines.push(`Процентная ставка: ${(input.interestRateBps / 100).toFixed(2)}%`);
  }
  if (input.loanCategoryLabel) {
    lines.push(`Категория: ${input.loanCategoryLabel}`);
  }
  if (input.displayName?.trim()) {
    lines.push(`Пользователь: ${input.displayName.trim()}`);
  }
  return lines.join("\n");
}

export function calendarReminderOffsetsMinutes(settings: CalendarReminderSettings): number[] {
  const offsets: number[] = [];
  if (settings.remindThreeDaysBefore) offsets.push(3 * 24 * 60);
  if (settings.remindOneDayBefore) offsets.push(24 * 60);
  if (settings.remindOnDueDay) offsets.push(0);
  return offsets;
}

function formatDateLabel(ms: number): string {
  const date = new Date(ms);
  return date.toLocaleDateString("ru-RU", { day: "numeric", month: "short", year: "numeric" });
}

export function debtsForCalendarDay<T extends { dateDueMs: number; isCompleted?: boolean }>(
  debts: T[],
  dayStartMs: number,
): T[] {
  const dayEnd = dayStartMs + 24 * 60 * 60 * 1000;
  return debts.filter((debt) => debt.dateDueMs >= dayStartMs && debt.dateDueMs < dayEnd);
}

import { describe, expect, it } from "vitest";
import {
  buildDebtCalendarDescription,
  buildDebtCalendarTitle,
  calendarReminderOffsetsMinutes,
  DEFAULT_CALENDAR_REMINDERS,
} from "./debtCalendar";

describe("debtCalendar", () => {
  it("builds creditor title", () => {
    const title = buildDebtCalendarTitle({
      id: "1",
      name: "Yusuf",
      debtType: "CREDITOR",
      remainingCents: 20_000,
      targetCents: 20_000,
      dateGivenMs: Date.UTC(2026, 5, 1),
      dateDueMs: Date.UTC(2026, 6, 15),
    });
    expect(title).toContain("Yusuf");
    expect(title).toContain("$200.00");
  });

  it("marks closed debts", () => {
    const title = buildDebtCalendarTitle({
      id: "1",
      name: "Amr",
      debtType: "DEBTOR",
      remainingCents: 0,
      targetCents: 10_000,
      dateGivenMs: Date.UTC(2026, 5, 1),
      dateDueMs: Date.UTC(2026, 6, 15),
      isCompleted: true,
    });
    expect(title.startsWith("[ЗАКРЫТ]")).toBe(true);
  });

  it("builds description with rate and category", () => {
    const description = buildDebtCalendarDescription({
      id: "1",
      name: "Bank",
      debtType: "DEBTOR",
      remainingCents: 50_000,
      targetCents: 50_000,
      dateGivenMs: Date.UTC(2026, 5, 1),
      dateDueMs: Date.UTC(2026, 6, 15),
      interestRateBps: 1250,
      loanCategoryLabel: "Кредитная карта",
      displayName: "Александр",
    });
    expect(description).toContain("12.50%");
    expect(description).toContain("Александр");
  });

  it("returns reminder offsets", () => {
    expect(calendarReminderOffsetsMinutes(DEFAULT_CALENDAR_REMINDERS)).toEqual([24 * 60, 0]);
  });
});

import { describe, expect, it } from "vitest";
import {
  createNote,
  dateInputValue,
  ensureNoteLineDates,
  millisFromDateInput,
  noteTotalCents,
  startOfTodayMillis,
  type WebNote,
} from "./notesStore";

describe("noteTotalCents", () => {
  it("sums financial lines", () => {
    const note: WebNote = {
      id: "n",
      title: "Рынок",
      kind: "FINANCIAL",
      body: "",
      mark: "NONE",
      closed: false,
      createdAt: 1,
      lines: [
        { id: "a", label: "Хлеб", amountCents: 20_000, occurredAt: 1 },
        { id: "b", label: "Молоко", amountCents: 30_000, occurredAt: 1 },
      ],
    };
    expect(noteTotalCents(note)).toBe(50_000);
  });
});

describe("note line dates", () => {
  it("fills missing dates from the note createdAt", () => {
    const note = ensureNoteLineDates({
      id: "n",
      title: "Акаи Сайод",
      kind: "FINANCIAL",
      body: "",
      mark: "NONE",
      closed: false,
      createdAt: 1_700_000_000_000,
      lines: [{ id: "a", label: "Zelle", amountCents: 7_500_00, occurredAt: 0 }],
    });
    expect(note.lines[0]?.occurredAt).toBe(1_700_000_000_000);
  });

  it("keeps an existing line date", () => {
    const note = ensureNoteLineDates({
      id: "n",
      title: "Акаи Сайод",
      kind: "FINANCIAL",
      body: "",
      mark: "NONE",
      closed: false,
      createdAt: 1,
      lines: [{ id: "a", label: "Zelle", amountCents: 2_000_00, occurredAt: 99 }],
    });
    expect(note.lines[0]?.occurredAt).toBe(99);
  });

  it("creates a financial note with today's date on the first line", () => {
    const now = Date.UTC(2026, 10, 2, 18, 0, 0);
    const note = createNote("Акаи Сайод", "FINANCIAL", now);
    expect(note.lines).toHaveLength(1);
    expect(note.lines[0]?.occurredAt).toBe(now);
  });

  it("round-trips a local calendar date", () => {
    const millis = millisFromDateInput("2026-11-02");
    expect(dateInputValue(millis)).toBe("2026-11-02");
    const noon = new Date(millis);
    expect(noon.getHours()).toBe(12);
  });

  it("uses noon today as the date-input fallback", () => {
    const now = new Date(2026, 10, 2, 9, 38, 0).getTime();
    const today = new Date(startOfTodayMillis(now));
    expect(today.getHours()).toBe(12);
    expect(today.getDate()).toBe(2);
  });
});

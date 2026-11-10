const NOTES_KEY = "money.webNotes.v1";

export type NoteKind = "TEXT" | "FINANCIAL";
export type NoteMark = "NONE" | "PAID" | "OWED";

export interface WebNoteLine {
  id: string;
  label: string;
  amountCents: number;
  occurredAt: number;
}

export interface WebNote {
  id: string;
  title: string;
  kind: NoteKind;
  body: string;
  mark: NoteMark;
  closed: boolean;
  lines: WebNoteLine[];
  createdAt: number;
}

export function startOfTodayMillis(now = Date.now()): number {
  const date = new Date(now);
  date.setHours(12, 0, 0, 0);
  return date.getTime();
}

export function dateInputValue(millis: number): string {
  const date = new Date(millis);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function millisFromDateInput(value: string, fallback = startOfTodayMillis()): number {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) return fallback;
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  return new Date(year, month - 1, day, 12, 0, 0, 0).getTime();
}

export function ensureNoteLineDates(note: WebNote): WebNote {
  const fallback = note.createdAt > 0 ? note.createdAt : startOfTodayMillis();
  return {
    ...note,
    lines: note.lines.map((line) => ({
      ...line,
      occurredAt:
        typeof line.occurredAt === "number" && Number.isFinite(line.occurredAt) && line.occurredAt > 0
          ? line.occurredAt
          : fallback,
    })),
  };
}

export function noteTotalCents(note: WebNote): number {
  return note.lines.reduce((sum, line) => sum + line.amountCents, 0);
}

export function loadNotes(): WebNote[] {
  if (typeof localStorage === "undefined") return [];
  try {
    const raw = localStorage.getItem(NOTES_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as WebNote[];
    return Array.isArray(parsed) ? parsed.map(ensureNoteLineDates) : [];
  } catch {
    return [];
  }
}

export function saveNotes(notes: WebNote[]): void {
  if (typeof localStorage === "undefined") return;
  localStorage.setItem(NOTES_KEY, JSON.stringify(notes));
}

export function createNote(title: string, kind: NoteKind, now = Date.now()): WebNote {
  return {
    id: crypto.randomUUID(),
    title: title.trim() || (kind === "FINANCIAL" ? "Расчёт" : "Заметка"),
    kind,
    body: "",
    mark: "NONE",
    closed: false,
    lines: kind === "FINANCIAL" ? [{ id: crypto.randomUUID(), label: "", amountCents: 0, occurredAt: now }] : [],
    createdAt: now,
  };
}

export function createNoteLine(now = Date.now()): WebNoteLine {
  return {
    id: crypto.randomUUID(),
    label: "",
    amountCents: 0,
    occurredAt: now,
  };
}

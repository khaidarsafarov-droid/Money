const NOTES_KEY = "money.webNotes.v1";

export type NoteKind = "TEXT" | "FINANCIAL";
export type NoteMark = "NONE" | "PAID" | "OWED";

export interface WebNoteLine {
  id: string;
  label: string;
  amountCents: number;
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

export function noteTotalCents(note: WebNote): number {
  return note.lines.reduce((sum, line) => sum + line.amountCents, 0);
}

export function loadNotes(): WebNote[] {
  if (typeof localStorage === "undefined") return [];
  try {
    const raw = localStorage.getItem(NOTES_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as WebNote[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function saveNotes(notes: WebNote[]): void {
  if (typeof localStorage === "undefined") return;
  localStorage.setItem(NOTES_KEY, JSON.stringify(notes));
}

export function createNote(title: string, kind: NoteKind): WebNote {
  return {
    id: crypto.randomUUID(),
    title: title.trim() || (kind === "FINANCIAL" ? "Расчёт" : "Заметка"),
    kind,
    body: "",
    mark: "NONE",
    closed: false,
    lines: kind === "FINANCIAL" ? [{ id: crypto.randomUUID(), label: "", amountCents: 0 }] : [],
    createdAt: Date.now(),
  };
}

import { useState } from "react";
import { NotebookPen, Plus, Trash2 } from "lucide-react";
import { AppIcon } from "@/components/ui/SurfaceCard";
import { EmptyState } from "@/components/ui/EmptyState";
import { formatMoney, parseMoneyInput } from "@/lib/money";
import {
  createNote,
  loadNotes,
  noteTotalCents,
  saveNotes,
  type NoteKind,
  type NoteMark,
  type WebNote,
} from "@/lib/notesStore";

interface NotesPanelProps {
  hideAmounts?: boolean;
}

export function NotesPanel({ hideAmounts = false }: NotesPanelProps) {
  const [notes, setNotes] = useState<WebNote[]>(() => loadNotes());
  const [openId, setOpenId] = useState<string | null>(null);
  const [title, setTitle] = useState("");
  const [kind, setKind] = useState<NoteKind>("TEXT");

  function update(next: WebNote[]) {
    setNotes(next);
    saveNotes(next);
  }

  const open = notes.find((note) => note.id === openId) ?? null;

  if (open) {
    return (
      <NoteEditor
        note={open}
        hideAmounts={hideAmounts}
        onBack={() => setOpenId(null)}
        onChange={(next) => update(notes.map((note) => (note.id === next.id ? next : note)))}
        onDelete={() => {
          update(notes.filter((note) => note.id !== open.id));
          setOpenId(null);
        }}
      />
    );
  }

  return (
    <section className="space-y-4">
      <h2 className="text-base font-semibold">Заметки ({notes.length})</h2>
      <form
        className="space-y-3 rounded-[24px] bg-white p-4 shadow-[0_8px_20px_rgb(15_110_92_/_0.06)]"
        onSubmit={(event) => {
          event.preventDefault();
          const note = createNote(title, kind);
          update([note, ...notes]);
          setTitle("");
          setOpenId(note.id);
        }}
      >
        <input
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="Название"
          className="w-full rounded-2xl bg-milk-bg px-4 py-3 text-sm outline-none"
        />
        <div className="flex gap-2">
          <KindButton selected={kind === "TEXT"} onClick={() => setKind("TEXT")} label="Текст" />
          <KindButton selected={kind === "FINANCIAL"} onClick={() => setKind("FINANCIAL")} label="Расчёт" />
        </div>
        <button type="submit" className="pill-btn-primary inline-flex items-center gap-1.5">
          <AppIcon icon={Plus} size={14} className="text-white" />
          Новая заметка
        </button>
      </form>

      {notes.length === 0 ? (
        <EmptyState
          icon={NotebookPen}
          title="Заметок пока нет"
          description="Текст или расчёт с итогом. Оплаченное отмечается зелёным, долг — красным."
        />
      ) : (
        <ul className="grid gap-3">
          {notes.map((note) => (
            <li key={note.id}>
              <button
                type="button"
                onClick={() => setOpenId(note.id)}
                className={`w-full rounded-[24px] bg-white p-4 text-left shadow-[0_8px_20px_rgb(15_110_92_/_0.06)] ${markClass(note.mark)}`}
              >
                <p className="text-sm text-milk-muted">{note.kind === "FINANCIAL" ? "Расчёт" : "Текст"}</p>
                <p className="text-lg font-semibold">{note.title}</p>
                {note.kind === "FINANCIAL" ? (
                  <p className={`text-sm font-medium ${markText(note.mark)}`}>
                    {formatMoney(noteTotalCents(note), { hidden: hideAmounts })}
                  </p>
                ) : (
                  <p className="truncate text-sm text-milk-muted">{note.body || "Пустая заметка"}</p>
                )}
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function NoteEditor({
  note,
  hideAmounts,
  onBack,
  onChange,
  onDelete,
}: {
  note: WebNote;
  hideAmounts: boolean;
  onBack: () => void;
  onChange: (note: WebNote) => void;
  onDelete: () => void;
}) {
  const total = noteTotalCents(note);
  return (
    <section className="space-y-4">
      <button type="button" onClick={onBack} className="text-sm font-semibold text-milk-accent">
        Назад к заметкам
      </button>
      <input
        value={note.title}
        onChange={(event) => onChange({ ...note, title: event.target.value })}
        className="w-full rounded-2xl bg-white px-4 py-3 text-lg font-semibold outline-none"
      />
      {note.kind === "TEXT" ? (
        <textarea
          value={note.body}
          onChange={(event) => onChange({ ...note, body: event.target.value })}
          rows={8}
          placeholder="Текст"
          className="w-full rounded-[24px] bg-white px-4 py-3 text-sm outline-none"
        />
      ) : (
        <div className="space-y-3 rounded-[24px] bg-white p-4">
          {note.lines.map((line) => (
            <div key={line.id} className="flex gap-2">
              <input
                value={line.label}
                onChange={(event) =>
                  onChange({
                    ...note,
                    lines: note.lines.map((item) =>
                      item.id === line.id ? { ...item, label: event.target.value } : item,
                    ),
                  })
                }
                placeholder="Строка"
                className="min-w-0 flex-1 rounded-2xl bg-milk-bg px-3 py-2 text-sm outline-none"
              />
              <input
                value={line.amountCents ? (line.amountCents / 100).toString() : ""}
                onChange={(event) => {
                  const cents = parseMoneyInput(event.target.value) ?? 0;
                  onChange({
                    ...note,
                    lines: note.lines.map((item) =>
                      item.id === line.id ? { ...item, amountCents: cents } : item,
                    ),
                  });
                }}
                inputMode="decimal"
                placeholder="0"
                className="w-24 rounded-2xl bg-milk-bg px-3 py-2 text-sm outline-none"
              />
              <button
                type="button"
                aria-label="Удалить строку"
                onClick={() => onChange({ ...note, lines: note.lines.filter((item) => item.id !== line.id) })}
                className="flex size-10 items-center justify-center rounded-full text-milk-muted"
              >
                <AppIcon icon={Trash2} size={14} />
              </button>
            </div>
          ))}
          <button
            type="button"
            className="pill-btn-secondary"
            onClick={() =>
              onChange({
                ...note,
                lines: [...note.lines, { id: crypto.randomUUID(), label: "", amountCents: 0 }],
              })
            }
          >
            Добавить строку
          </button>
          <p className={`text-lg font-semibold ${markText(note.mark)}`}>
            Итого {formatMoney(total, { hidden: hideAmounts })}
          </p>
          <div className="flex gap-2">
            <MarkButton
              label="Оплачено"
              selected={note.mark === "PAID"}
              className="text-milk-success"
              onClick={() => onChange({ ...note, mark: toggleMark(note.mark, "PAID") })}
            />
            <MarkButton
              label="Должен"
              selected={note.mark === "OWED"}
              className="text-milk-error"
              onClick={() => onChange({ ...note, mark: toggleMark(note.mark, "OWED") })}
            />
          </div>
        </div>
      )}
      <button type="button" onClick={onDelete} className="text-sm font-medium text-milk-error">
        Удалить заметку
      </button>
    </section>
  );
}

function toggleMark(current: NoteMark, next: NoteMark): NoteMark {
  return current === next ? "NONE" : next;
}

function markClass(mark: NoteMark): string {
  if (mark === "PAID") return "ring-1 ring-milk-success/40";
  if (mark === "OWED") return "ring-1 ring-milk-error/40";
  return "";
}

function markText(mark: NoteMark): string {
  if (mark === "PAID") return "text-milk-success";
  if (mark === "OWED") return "text-milk-error";
  return "text-milk-text";
}

function KindButton({ selected, onClick, label }: { selected: boolean; onClick: () => void; label: string }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex-1 rounded-full px-3 py-2 text-sm font-medium ${selected ? "bg-milk-accent text-white" : "bg-milk-bg text-milk-muted"}`}
    >
      {label}
    </button>
  );
}

function MarkButton({
  label,
  selected,
  className,
  onClick,
}: {
  label: string;
  selected: boolean;
  className: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex-1 rounded-full border px-3 py-2 text-sm font-medium ${className} ${selected ? "border-current bg-milk-bg" : "border-milk-surface"}`}
    >
      {label}
    </button>
  );
}

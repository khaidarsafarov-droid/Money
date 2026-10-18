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
        className="space-y-3 rounded-[24px] bg-surface-container-lowest p-4 shadow-[0_8px_20px_rgb(15_110_92_/_0.06)]"
        onSubmit={(event) => {
          event.preventDefault();
          const note = createNote(title, kind);
          update([note, ...notes]);
          setTitle("");
          setOpenId(note.id);
        }}
      >
        <label className="block space-y-1.5">
          <span className="text-sm text-on-surface-variant">Название</span>
          <input
            name="note-title"
            autoComplete="off"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            placeholder="Название"
            className="field-control w-full"
          />
        </label>
        <div className="flex gap-2">
          <KindButton selected={kind === "TEXT"} onClick={() => setKind("TEXT")} label="Текст" />
          <KindButton selected={kind === "FINANCIAL"} onClick={() => setKind("FINANCIAL")} label="Расчёт" />
        </div>
        <button type="submit" className="pill-btn-primary inline-flex items-center gap-1.5">
          <AppIcon icon={Plus} size={14} className="text-on-primary" />
          Новая заметка
        </button>
      </form>

      {notes.length === 0 ? (
        <EmptyState
          icon={NotebookPen}
          title="Заметок пока нет"
        />
      ) : (
        <ul className="grid gap-3">
          {notes.map((note) => (
            <li key={note.id}>
              <button
                type="button"
                onClick={() => setOpenId(note.id)}
                className={`w-full rounded-[24px] bg-surface-container-lowest p-4 text-left shadow-[0_8px_20px_rgb(15_110_92_/_0.06)] ${markClass(note.mark)}`}
              >
                <p className="text-sm text-on-surface-variant">{note.kind === "FINANCIAL" ? "Расчёт" : "Текст"}</p>
                <p className="text-lg font-semibold">{note.title}</p>
                {note.kind === "FINANCIAL" ? (
                  <p className={`text-sm font-medium ${markText(note.mark)}`}>
                    {formatMoney(noteTotalCents(note), { hidden: hideAmounts })}
                  </p>
                ) : (
                  <p className="truncate text-sm text-on-surface-variant">{note.body || "Пустая заметка"}</p>
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
      <button
        type="button"
        onClick={onBack}
        className="inline-flex min-h-12 items-center text-sm font-semibold text-primary focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
      >
        Назад к заметкам
      </button>
      <label className="block space-y-1.5">
        <span className="text-sm text-on-surface-variant">Название</span>
        <input
          name="note-title"
          autoComplete="off"
          value={note.title}
          onChange={(event) => onChange({ ...note, title: event.target.value })}
          className="field-control w-full text-lg font-semibold"
        />
      </label>
      {note.kind === "TEXT" ? (
        <label className="block space-y-1.5">
          <span className="text-sm text-on-surface-variant">Текст</span>
          <textarea
            name="note-body"
            value={note.body}
            onChange={(event) => onChange({ ...note, body: event.target.value })}
            rows={8}
            placeholder="Текст"
            className="field-control w-full rounded-[24px] bg-surface-container-lowest"
          />
        </label>
      ) : (
        <div className="space-y-3 rounded-[24px] bg-surface-container-lowest p-4">
          <div className="flex gap-2 text-sm text-on-surface-variant">
            <span className="min-w-0 flex-1">Строка</span>
            <span className="w-24">Сумма (USD)</span>
            <span className="w-12" />
          </div>
          {note.lines.map((line) => (
            <div key={line.id} className="flex items-center gap-2">
              <input
                aria-label="Строка"
                name="line-label"
                autoComplete="off"
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
                className="field-control min-w-0 flex-1"
              />
              <input
                aria-label="Сумма"
                name="line-amount"
                autoComplete="off"
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
                className="field-control w-24"
              />
              <button
                type="button"
                aria-label="Удалить строку"
                onClick={() => onChange({ ...note, lines: note.lines.filter((item) => item.id !== line.id) })}
                className="flex size-12 shrink-0 items-center justify-center rounded-full text-on-surface-variant focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
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
              className="text-primary"
              onClick={() => onChange({ ...note, mark: toggleMark(note.mark, "PAID") })}
            />
            <MarkButton
              label="Должен"
              selected={note.mark === "OWED"}
              className="text-error"
              onClick={() => onChange({ ...note, mark: toggleMark(note.mark, "OWED") })}
            />
          </div>
        </div>
      )}
      <button
        type="button"
        onClick={onDelete}
        className="inline-flex min-h-12 items-center text-sm font-medium text-error focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
      >
        Удалить заметку
      </button>
    </section>
  );
}

function toggleMark(current: NoteMark, next: NoteMark): NoteMark {
  return current === next ? "NONE" : next;
}

function markClass(mark: NoteMark): string {
  if (mark === "PAID") return "ring-1 ring-primary/40";
  if (mark === "OWED") return "ring-1 ring-error/40";
  return "";
}

function markText(mark: NoteMark): string {
  if (mark === "PAID") return "text-primary";
  if (mark === "OWED") return "text-error";
  return "text-on-surface";
}

function KindButton({ selected, onClick, label }: { selected: boolean; onClick: () => void; label: string }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={selected}
      className={`min-h-12 flex-1 rounded-full px-3 py-2 text-sm font-medium focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary ${selected ? "bg-primary text-on-primary" : "bg-background text-on-surface-variant"}`}
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
      aria-pressed={selected}
      className={`min-h-12 flex-1 rounded-full border px-3 py-2 text-sm font-medium focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary ${className} ${selected ? "border-current bg-background" : "border-outline-variant"}`}
    >
      {label}
    </button>
  );
}

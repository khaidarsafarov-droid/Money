import { HandCoins, MoreHorizontal, Share2, Zap } from "lucide-react";
import { useState } from "react";
import { isDebtOverdue } from "@/lib/financeEngine";
import { suggestAcceleratePaymentCents } from "@/lib/debtFreedomEngine";
import { shareDebtReminder } from "@/lib/debtRemind";
import { AppIcon } from "@/components/ui/SurfaceCard";
import { FinanceCardShell } from "@/components/cards/FinanceCardShell";
import type { FinancialItem } from "@/types/finance";
import { debtRemainingCents } from "@/types/finance";

interface DebtCardProps {
  item: FinancialItem;
  hideAmounts?: boolean;
  onAddPayment?: (item: FinancialItem, prefillCents?: number) => void;
  onAddAdvance?: (amountCents: number, givenAt: string, note: string) => void;
  onRecordPayment?: (amountCents: number, paidAt: string) => void;
  showHistory?: boolean;
  onDelete?: (item: FinancialItem) => void;
}

export function DebtCard({
  item,
  hideAmounts = false,
  onAddPayment,
  onAddAdvance,
  onRecordPayment,
  showHistory = false,
  onDelete,
}: DebtCardProps) {
  const [menuOpen, setMenuOpen] = useState(false);
  const canPay = item.status !== "COMPLETED" && onAddPayment;
  const overdue = isDebtOverdue(item);
  const remaining = Math.max(item.target - item.amount, 0);
  const accelerateCents = suggestAcceleratePaymentCents(remaining, item.target, item.amount);

  return (
    <div className="space-y-2">
    <FinanceCardShell
      item={item}
      typeLabel="Долг"
      typeIcon={HandCoins}
      remainingLabel="Осталось"
      paidLabel="Внесено"
      overdue={overdue}
      hideAmounts={hideAmounts}
      onDelete={onDelete}
      primaryAmountCents={remaining}
      menu={
        <div className="relative">
          <button
            type="button"
            onClick={() => setMenuOpen((open) => !open)}
            className="flex min-h-12 min-w-12 items-center justify-center rounded-md text-on-surface-variant transition hover:bg-background focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
            aria-label="Ещё"
          >
            <AppIcon icon={MoreHorizontal} size={18} />
          </button>
          {menuOpen ? (
            <>
              <button
                type="button"
                aria-label="Закрыть меню"
                className="fixed inset-0 z-10"
                onClick={() => setMenuOpen(false)}
              />
              <div className="absolute right-0 z-20 mt-1 min-w-40 rounded-md border border-outline-variant bg-surface-container-lowest py-1 shadow-sm">
                {canPay ? (
                  <button
                    type="button"
                    className="flex min-h-12 w-full items-center gap-2 px-3 text-left text-sm hover:bg-background focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                    onClick={() => {
                      setMenuOpen(false);
                      onAddPayment?.(item, accelerateCents);
                    }}
                  >
                    <AppIcon icon={Zap} size={14} />
                    Ускорить
                  </button>
                ) : null}
                <button
                  type="button"
                  className="flex min-h-12 w-full items-center gap-2 px-3 text-left text-sm hover:bg-background focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                  onClick={() => {
                    setMenuOpen(false);
                    void shareDebtReminder(item);
                  }}
                >
                  <AppIcon icon={Share2} size={14} />
                  Напомнить
                </button>
              </div>
            </>
          ) : null}
        </div>
      }
      meta={
        item.dateDue ? (
          <p className={`text-subheading ${overdue ? "font-medium text-error" : ""}`}>
            Срок: {new Date(item.dateDue).toLocaleDateString("ru-RU")}
          </p>
        ) : null
      }
      action={
        canPay ? (
          <button
            type="button"
            onClick={() => onAddPayment(item)}
            className="pill-btn-primary inline-flex w-full items-center justify-center gap-2"
          >
            <AppIcon icon={HandCoins} size={14} className="text-on-primary" />
            Добавить оплату
          </button>
        ) : null
      }
    />
    {showHistory ? (
      <DebtHistory
        item={item}
        hideAmounts={hideAmounts}
        onAddAdvance={onAddAdvance}
        onRecordPayment={onRecordPayment}
      />
    ) : null}
    </div>
  );
}

function DebtHistory({
  item,
  hideAmounts,
  onAddAdvance,
  onRecordPayment,
}: {
  item: FinancialItem;
  hideAmounts: boolean;
  onAddAdvance?: (amountCents: number, givenAt: string, note: string) => void;
  onRecordPayment?: (amountCents: number, paidAt: string) => void;
}) {
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<"none" | "took" | "returned">("none");
  const [amount, setAmount] = useState("");
  const [givenAt, setGivenAt] = useState(() => new Date().toISOString().slice(0, 10));
  const [note, setNote] = useState("");
  const [error, setError] = useState<string | null>(null);
  const advances = (item.advances ?? []).filter((advance) => !advance.deleted && advance.amount > 0);
  const payments = item.history.filter((payment) => !payment.deleted && payment.amount > 0);
  const remaining = debtRemainingCents(
    advances.map((advance) => advance.amount),
    payments.map((payment) => payment.amount),
  );
  const payable = Math.max(item.target - item.amount, 0);
  const rows = [
    ...advances.map((advance) => ({
      id: advance.id,
      label: "Взял",
      amount: advance.amount,
      at: advance.givenAt,
      note: advance.note,
    })),
    ...payments.map((payment) => ({
      id: payment.id,
      label: "Отдал",
      amount: payment.amount,
      at: payment.paidAt,
      note: "",
    })),
  ].sort((a, b) => a.at.localeCompare(b.at));

  return (
    <div className="rounded-2xl bg-surface-container-lowest px-4 py-2">
      <button
        type="button"
        className="flex min-h-12 w-full items-center justify-between text-left text-sm font-medium"
        onClick={() => setOpen((value) => !value)}
      >
        История
        <span className="text-on-surface-variant">
          {hideAmounts ? "••••" : `${(remaining / 100).toFixed(2)} $`}
        </span>
      </button>
      {open ? (
        <div className="space-y-2 pb-3">
          {rows.length === 0 ? <p className="text-sm text-on-surface-variant">Пока нет операций</p> : null}
          {rows.map((row) => (
            <div key={row.id} className="flex items-baseline justify-between gap-3 text-sm">
              <span>
                {row.label} {hideAmounts ? "••••" : `${(row.amount / 100).toFixed(2)} $`}
                {row.note ? <span className="block text-xs text-on-surface-variant">{row.note}</span> : null}
              </span>
              <span className="text-on-surface-variant">{new Date(row.at).toLocaleDateString("ru-RU")}</span>
            </div>
          ))}
          {form === "none" ? (
            <div className="grid grid-cols-2 gap-2 pt-2">
              <button
                type="button"
                className="history-took min-h-12 rounded-full px-4 text-sm font-semibold focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                onClick={() => {
                  setForm("took");
                  setError(null);
                }}
              >
                Взял
              </button>
              <button
                type="button"
                className="pill-btn-primary"
                onClick={() => {
                  setForm("returned");
                  setError(null);
                }}
              >
                Отдал
              </button>
            </div>
          ) : (
            <form
              className="grid gap-2 pt-2"
              onSubmit={(event) => {
                event.preventDefault();
                const cents = Math.round(Number(amount.replace(",", ".")) * 100);
                if (!Number.isFinite(cents) || cents <= 0) {
                  setError("Сумма должна быть больше 0");
                  return;
                }
                if (form === "returned" && cents > payable) {
                  setError("Сумма превышает остаток долга");
                  return;
                }
                if (form === "took") {
                  onAddAdvance?.(cents, givenAt, note.trim());
                } else {
                  onRecordPayment?.(cents, givenAt);
                }
                setAmount("");
                setNote("");
                setError(null);
                setForm("none");
              }}
            >
              <input
                value={amount}
                onChange={(event) => setAmount(event.target.value)}
                inputMode="decimal"
                placeholder="Сумма"
                aria-label="Сумма"
                className="min-h-12 rounded-xl border border-outline-variant bg-transparent px-3"
              />
              {form === "returned" ? (
                <button
                  type="button"
                  className="min-h-12 text-left text-sm font-medium text-primary"
                  onClick={() => {
                    setAmount((payable / 100).toFixed(2));
                    setError(null);
                  }}
                >
                  Весь остаток
                </button>
              ) : null}
              <input
                type="date"
                value={givenAt}
                onChange={(event) => setGivenAt(event.target.value)}
                aria-label="Дата"
                className="min-h-12 rounded-xl border border-outline-variant bg-transparent px-3"
              />
              {form === "took" ? (
                <input
                  value={note}
                  onChange={(event) => setNote(event.target.value)}
                  placeholder="Заметка"
                  aria-label="Заметка"
                  className="min-h-12 rounded-xl border border-outline-variant bg-transparent px-3"
                />
              ) : null}
              {error ? <p className="text-sm text-error">{error}</p> : null}
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  className="pill-btn-secondary"
                  onClick={() => {
                    setForm("none");
                    setError(null);
                  }}
                >
                  Отмена
                </button>
                <button type="submit" className="pill-btn-primary">
                  Сохранить
                </button>
              </div>
            </form>
          )}
        </div>
      ) : null}
    </div>
  );
}

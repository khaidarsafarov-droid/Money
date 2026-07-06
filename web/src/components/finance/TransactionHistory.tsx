import { ArrowDownLeft, ArrowUpRight, PiggyBank } from "lucide-react";
import { AppIcon } from "@/components/ui/SurfaceCard";
import { formatMoney } from "@/lib/money";
import type { Transaction, TransactionKind } from "@/types/finance";
import type { FinancialItem } from "@/types/finance";

interface TransactionHistoryProps {
  transactions: Transaction[];
  goals: FinancialItem[];
}

function labelForTransaction(tx: Transaction, goals: FinancialItem[]): string {
  switch (tx.kind) {
    case "DEPOSIT":
      return tx.note ?? "Пополнение";
    case "EXPENSE":
      return tx.category ? `Расход · ${tx.category}` : "Расход";
    case "GOAL_TRANSFER": {
      const goal = goals.find((item) => item.id === tx.goalId);
      return goal ? `Перевод · ${goal.name}` : "Перевод в цель";
    }
    default:
      return "Операция";
  }
}

function isPositiveTransaction(kind: TransactionKind): boolean {
  return kind === "DEPOSIT" || kind === "GOAL_TRANSFER";
}

export function TransactionHistory({ transactions, goals }: TransactionHistoryProps) {
  if (transactions.length === 0) return null;

  return (
    <section className="space-y-3">
      <h2 className="text-lg font-semibold text-milk-text">Операции</h2>
      <ul className="divide-y divide-milk-surface/80 overflow-hidden rounded-2xl border border-milk-surface bg-milk-bg/60">
        {transactions.slice(0, 12).map((tx) => {
          const positive = isPositiveTransaction(tx.kind);
          return (
            <li
              key={tx.id}
              className="flex items-center justify-between gap-3 px-4 py-3 text-sm"
            >
              <div className="flex min-w-0 items-center gap-3">
                <span
                  className={`inline-flex size-8 shrink-0 items-center justify-center rounded-full ${
                    positive
                      ? "bg-finance-positive-soft text-finance-positive"
                      : "bg-finance-negative-soft text-finance-negative"
                  }`}
                >
                  <AppIcon
                    icon={
                      tx.kind === "EXPENSE"
                        ? ArrowUpRight
                        : tx.kind === "GOAL_TRANSFER"
                          ? PiggyBank
                          : ArrowDownLeft
                    }
                    size={16}
                  />
                </span>
                <div className="min-w-0">
                  <p className="truncate font-medium text-milk-text">
                    {labelForTransaction(tx, goals)}
                  </p>
                  <p className="text-xs text-milk-muted">
                    {new Date(tx.createdAt).toLocaleString("ru-RU")}
                  </p>
                </div>
              </div>
              <span
                className={`shrink-0 font-semibold tabular-nums ${
                  positive ? "text-finance-positive" : "text-finance-negative"
                }`}
              >
                {positive ? "+" : "−"}
                {formatMoney(tx.amount)}
              </span>
            </li>
          );
        })}
      </ul>
    </section>
  );
}

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
    default: {
      const unreachable: never = tx.kind;
      return unreachable;
    }
  }
}

function isPositiveTransaction(kind: TransactionKind): boolean {
  return kind === "DEPOSIT" || kind === "GOAL_TRANSFER";
}

export function TransactionHistory({ transactions, goals }: TransactionHistoryProps) {
  if (transactions.length === 0) return null;

  return (
    <section className="space-y-2">
      <ul className="space-y-2">
        {transactions.map((tx) => {
          const positive = isPositiveTransaction(tx.kind);
          return (
            <li
              key={tx.id}
              className="flex items-center justify-between gap-3 rounded-[22px] bg-white px-3 py-3 text-sm shadow-[0_8px_24px_rgb(15_110_92_/_0.05)]"
            >
              <div className="flex min-w-0 items-center gap-3">
                <span
                  className={`flex size-10 shrink-0 items-center justify-center rounded-full ${
                    positive ? "bg-pastel-mint" : "bg-pastel-peach"
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
                    className={positive ? "text-finance-positive" : "text-finance-negative"}
                  />
                </span>
                <div className="min-w-0">
                  <p className="truncate text-milk-text">
                    {labelForTransaction(tx, goals)}
                  </p>
                  <p className="text-xs text-milk-muted">
                    {new Date(tx.createdAt).toLocaleString("ru-RU")}
                  </p>
                </div>
              </div>
              <span
                className={`shrink-0 tabular-nums ${
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

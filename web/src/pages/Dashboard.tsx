import { AnimatePresence, motion } from "framer-motion";
import { HandCoins, Inbox, Target, TrendingUp } from "lucide-react";
import { useCallback, useState } from "react";
import { FinanceCard } from "@/components/cards/FinanceCard";
import { BalanceOverviewCard } from "@/components/finance/BalanceOverviewCard";
import { TransactionHistory } from "@/components/finance/TransactionHistory";
import { AmountModal } from "@/components/modals/AmountModal";
import { ConfirmDeleteModal } from "@/components/modals/ConfirmDeleteModal";
import { DebtFormModal } from "@/components/modals/DebtFormModal";
import { GoalFormModal } from "@/components/modals/GoalFormModal";
import { PaymentModal } from "@/components/modals/PaymentModal";
import { EmptyState } from "@/components/ui/EmptyState";
import { ExpandableFab } from "@/components/ui/ExpandableFab";
import { StatCard } from "@/components/ui/StatCard";
import { StatusFilterTabs } from "@/components/ui/StatusFilterTabs";
import { TypeFilterChips } from "@/components/ui/TypeFilterChips";
import { SyncPanel } from "@/components/sync/SyncPanel";
import { useToast } from "@/components/ui/Toast";
import { useFinanceManager } from "@/hooks/useFinanceManager";
import { useWebSync } from "@/hooks/useWebSync";
import { usePaymentDeepLink } from "@/hooks/usePaymentDeepLink";
import { formatMoney } from "@/lib/money";
import type { FinancialItem } from "@/types/finance";

type AmountAction = "deposit" | "expense" | "goal-transfer";

export function DashboardPage() {
  const finance = useFinanceManager();
  const { showToast } = useToast();
  const webSync = useWebSync(finance.replaceState);
  const [paymentItem, setPaymentItem] = useState<FinancialItem | null>(null);
  const [transferGoal, setTransferGoal] = useState<FinancialItem | null>(null);
  const [amountAction, setAmountAction] = useState<AmountAction | null>(null);
  const [showAddGoal, setShowAddGoal] = useState(false);
  const [showAddDebt, setShowAddDebt] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<FinancialItem | null>(null);

  const goals = finance.state.items.filter((item) => item.type === "GOAL");
  const hasOperations =
    finance.transactions.length > 0 || finance.state.items.length > 0;

  const openPayment = useCallback((item: FinancialItem) => {
    setPaymentItem(item);
  }, []);

  usePaymentDeepLink(finance.state.items, openPayment);

  const availableHint = `Доступно на общем остатке: ${formatMoney(finance.globalFinance.availableBalanceCents)}`;

  const emptyDescription =
    finance.statusFilter === "completed"
      ? "Завершённые цели и погашенные долги появятся здесь."
      : finance.typeFilter === "debt"
        ? "Добавьте первый долг, чтобы отслеживать возвраты."
        : finance.typeFilter === "goal"
          ? "Создайте цель и начните копить на важную задачу."
          : "Создайте цель или добавьте долг — всё в одном месте.";

  return (
    <div className="mx-auto min-h-dvh max-w-5xl px-4 pb-32 pt-8 sm:px-6">
      <header className="mb-8 space-y-6">
        <div className="space-y-1">
          <p className="text-xs font-medium uppercase tracking-[0.25em] text-milk-muted">
            TruckerLoad · Project Money
          </p>
          <h1 className="text-3xl font-semibold tracking-tight text-milk-text sm:text-4xl">
            Личные цели
          </h1>
          <p className="text-sm text-milk-muted">USD · точность до центов</p>
        </div>

        <BalanceOverviewCard
          finance={finance.globalFinance}
          formatUsd={finance.formatUsd}
        />

        <SyncPanel
          peerUrl={webSync.peerUrl}
          onPeerUrlChange={webSync.setPeerUrl}
          status={webSync.status}
          lastError={webSync.lastError}
          lastSyncedAt={webSync.lastSyncedAt}
          onSync={() => {
            void webSync.syncNow(finance.state).then((ok) => {
              if (ok) showToast("Данные синхронизированы с телефоном");
            });
          }}
        />

        <div className="grid gap-4 sm:grid-cols-3">
          <StatCard
            label="Долги"
            value={finance.formatUsd(finance.dashboardTotals.totalDebtRemainingUsd)}
            icon={HandCoins}
            hint="Осталось получить"
          />
          <StatCard
            label="Свободно"
            value={finance.formatUsd(finance.dashboardTotals.availableBalanceUsd)}
            icon={TrendingUp}
            hint="Для переводов и расходов"
          />
          <StatCard
            label="Активных"
            value={String(finance.dashboardTotals.activeCount)}
            icon={Target}
            hint={`Завершено: ${finance.dashboardTotals.completedCount}`}
          />
        </div>
      </header>

      <main className="space-y-8">
        {hasOperations ? (
          <TransactionHistory transactions={finance.transactions} goals={goals} />
        ) : null}

        <div className="space-y-5">
          <div className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <h2 className="text-lg font-semibold text-milk-text">
                Записи ({finance.visibleItems.length})
              </h2>
              <StatusFilterTabs
                value={finance.statusFilter}
                onChange={finance.setStatusFilter}
              />
            </div>
            <TypeFilterChips value={finance.typeFilter} onChange={finance.setTypeFilter} />
          </div>

          {finance.visibleItems.length === 0 ? (
            <EmptyState
              icon={Inbox}
              title="Пока пусто"
              description={emptyDescription}
              action={
                finance.statusFilter !== "completed" ? (
                  <>
                    <button
                      type="button"
                      onClick={() => setShowAddGoal(true)}
                      className="pill-btn-primary inline-flex items-center gap-2"
                    >
                      <Target size={16} strokeWidth={1.5} />
                      Новая цель
                    </button>
                    <button
                      type="button"
                      onClick={() => setShowAddDebt(true)}
                      className="pill-btn-secondary inline-flex items-center gap-2"
                    >
                      <HandCoins size={16} strokeWidth={1.5} />
                      Новый долг
                    </button>
                  </>
                ) : undefined
              }
            />
          ) : (
            <motion.div layout className="grid gap-4 lg:grid-cols-2">
              <AnimatePresence mode="popLayout">
                {finance.visibleItems.map((item) => (
                  <FinanceCard
                    key={item.id}
                    item={item}
                    onAddPayment={openPayment}
                    onGoalTransfer={setTransferGoal}
                    onGoalExpense={() => setAmountAction("expense")}
                    onDelete={setPendingDelete}
                  />
                ))}
              </AnimatePresence>
            </motion.div>
          )}
        </div>
      </main>

      <PaymentModal
        item={paymentItem}
        open={paymentItem !== null}
        onClose={() => setPaymentItem(null)}
        onSubmit={(id, amount) => {
          const result = finance.processPayment(id, amount);
          if (result.ok) {
            showToast("Операция сохранена");
          }
          return result;
        }}
      />

      <AmountModal
        open={transferGoal !== null}
        title={transferGoal ? `Перевод — ${transferGoal.name}` : "Перевод"}
        hint={availableHint}
        submitLabel="Перевести"
        onClose={() => setTransferGoal(null)}
        onSubmit={(amount) => {
          if (!transferGoal) return { ok: false, error: "Цель не выбрана" };
          const result = finance.addGoalTransfer(transferGoal.id, amount);
          if (result.ok) showToast("Перевод в цель выполнен");
          return result;
        }}
      />

      <AmountModal
        open={amountAction === "deposit"}
        title="Пополнить баланс"
        hint="Средства попадут на общий остаток и станут доступны для переводов в цели."
        submitLabel="Пополнить"
        onClose={() => setAmountAction(null)}
        onSubmit={(amount) => {
          const result = finance.addDeposit(amount);
          if (result.ok) showToast("Баланс пополнен");
          return result;
        }}
      />

      <AmountModal
        open={amountAction === "expense"}
        title="Общий расход"
        hint={availableHint}
        submitLabel="Списать"
        onClose={() => setAmountAction(null)}
        onSubmit={(amount) => {
          const result = finance.addExpense(amount);
          if (result.ok) showToast("Расход сохранён");
          return result;
        }}
      />

      <GoalFormModal
        open={showAddGoal}
        onClose={() => setShowAddGoal(false)}
        onSubmit={(input) => {
          finance.addGoal(input);
          showToast("Цель добавлена");
        }}
      />

      <DebtFormModal
        open={showAddDebt}
        onClose={() => setShowAddDebt(false)}
        onSubmit={(input) => {
          finance.addDebt(input);
          showToast("Долг добавлен");
        }}
      />

      <ConfirmDeleteModal
        open={pendingDelete !== null}
        title="Удалить запись?"
        message={
          pendingDelete
            ? `«${pendingDelete.name}» будет удалена без возможности восстановления.`
            : ""
        }
        onClose={() => setPendingDelete(null)}
        onConfirm={() => {
          if (pendingDelete) {
            finance.removeItem(pendingDelete.id);
            showToast("Запись удалена", "info");
          }
        }}
      />

      <ExpandableFab
        onAddGoal={() => setShowAddGoal(true)}
        onAddDebt={() => setShowAddDebt(true)}
        onAddDeposit={() => setAmountAction("deposit")}
        onAddExpense={() => setAmountAction("expense")}
      />
    </div>
  );
}

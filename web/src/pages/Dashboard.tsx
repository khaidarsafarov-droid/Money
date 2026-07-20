import { AnimatePresence } from "framer-motion";
import { HandCoins, Inbox, Target, TrendingUp } from "lucide-react";
import { useCallback, useState } from "react";
import { personalizedGreeting } from "@money/shared";
import { OnboardingFlow } from "@/components/onboarding/OnboardingFlow";
import { FreedomForecastCard } from "@/components/debt/FreedomForecastCard";
import { FinanceCard } from "@/components/cards/FinanceCard";
import { BalanceOverviewCard } from "@/components/finance/BalanceOverviewCard";
import { TransactionHistory } from "@/components/finance/TransactionHistory";
import { AmountModal } from "@/components/modals/AmountModal";
import { ConfirmDeleteModal } from "@/components/modals/ConfirmDeleteModal";
import { DebtFormModal } from "@/components/modals/DebtFormModal";
import { GoalFormModal } from "@/components/modals/GoalFormModal";
import { PaymentModal } from "@/components/modals/PaymentModal";
import { DebtAdvisorSheet } from "@/components/psychology/DebtAdvisorSheet";
import { GamificationBanner } from "@/components/psychology/GamificationBanner";
import { PsychologySettingsPanel } from "@/components/psychology/PsychologySettingsPanel";
import { EmptyState } from "@/components/ui/EmptyState";
import { ExpandableFab } from "@/components/ui/ExpandableFab";
import { AppIcon } from "@/components/ui/SurfaceCard";
import { StatCard } from "@/components/ui/StatCard";
import { StatusFilterTabs } from "@/components/ui/StatusFilterTabs";
import { TypeFilterChips } from "@/components/ui/TypeFilterChips";
import { SyncPanel } from "@/components/sync/SyncPanel";
import { useToast } from "@/components/ui/Toast";
import { useUserProfile } from "@/hooks/useUserProfile";
import { useAppPsychology } from "@/hooks/useAppPsychology";
import { useFinanceManager } from "@/hooks/useFinanceManager";
import { useWebSync } from "@/hooks/useWebSync";
import { usePaymentDeepLink } from "@/hooks/usePaymentDeepLink";
import { formatMoney, formatUsd, parseMoneyInput } from "@/lib/money";
import type { FinancialItem } from "@/types/finance";

type AmountAction = "deposit" | "expense" | "goal-transfer";

export function DashboardPage() {
  const finance = useFinanceManager();
  const psychology = useAppPsychology();
  const { profile: userProfile } = useUserProfile();
  const { showToast } = useToast();
  const webSync = useWebSync(finance.replaceState, () => finance.state);
  const [paymentItem, setPaymentItem] = useState<FinancialItem | null>(null);
  const [paymentPrefillCents, setPaymentPrefillCents] = useState(0);
  const [transferGoal, setTransferGoal] = useState<FinancialItem | null>(null);
  const [amountAction, setAmountAction] = useState<AmountAction | null>(null);
  const [showAddGoal, setShowAddGoal] = useState(false);
  const [showAddDebt, setShowAddDebt] = useState(false);
  const [showAdvisor, setShowAdvisor] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<FinancialItem | null>(null);

  const hideAmounts = psychology.privacy.hideAmounts;
  const money = useCallback(
    (cents: number) => formatMoney(cents, { hidden: hideAmounts }),
    [hideAmounts],
  );
  const usd = useCallback(
    (amount: number) => formatUsd(amount, { hidden: hideAmounts }),
    [hideAmounts],
  );

  const goals = finance.state.items.filter((item) => item.type === "GOAL");
  const activeDebts = finance.state.items.filter(
    (item) => item.type === "DEBT" && item.status !== "COMPLETED",
  );
  const hasOperations =
    finance.transactions.length > 0 || finance.state.items.length > 0;

  const openPayment = useCallback((item: FinancialItem, prefillCents = 0) => {
    setPaymentPrefillCents(prefillCents);
    setPaymentItem(item);
  }, []);

  usePaymentDeepLink(finance.state.items, openPayment);

  const availableHint = `Доступно на общем остатке: ${money(finance.globalFinance.availableBalanceCents)}`;

  const applyAutopilot = useCallback(() => {
    const plan = psychology.planAutopilotPayment(finance.state.items);
    if (!plan) {
      showToast("Не удалось применить микроплатёж", "info");
      return;
    }
    const amountInput = (plan.amountCents / 100).toFixed(2);
    const item = finance.state.items.find((entry) => entry.id === plan.debtId);
    const result = finance.processPayment(plan.debtId, amountInput);
    if (result.ok && item) {
      const paymentCents = parseMoneyInput(amountInput);
      if (paymentCents) {
        psychology.recordDebtPayment(item, paymentCents);
      }
      showToast("Операция сохранена");
    } else if (!result.ok) {
      showToast(result.error ?? "Не удалось применить платёж", "info");
    }
  }, [finance, psychology, showToast]);

  const emptyDescription =
    finance.statusFilter === "completed"
      ? "Завершённые цели и погашенные долги появятся здесь."
      : finance.typeFilter === "debt"
        ? "Добавьте первый долг, чтобы отслеживать возвраты."
        : finance.typeFilter === "goal"
          ? "Создайте цель и начните копить на важную задачу."
          : "Создайте цель или добавьте долг — всё в одном месте.";

  if (!userProfile.onboardingCompleted) {
    return <OnboardingFlow />;
  }

  const debtProgressPercent =
    activeDebts.length > 0
      ? Math.min(
          100,
          Math.floor(
            (activeDebts.reduce((sum, d) => sum + d.currentCents, 0) /
              Math.max(
                1,
                activeDebts.reduce((sum, d) => sum + d.targetCents, 0),
              )) *
              100,
          ),
        )
      : 0;

  return (
    <div className="mx-auto min-h-dvh max-w-5xl px-4 pb-28 pt-6 sm:px-6">
      <header className="mb-6 space-y-4">
        <div className="space-y-0.5">
          <p className="text-xs text-milk-muted">TruckerLoad · Project Money</p>
          <h1 className="text-2xl font-medium tracking-tight text-milk-text sm:text-3xl">
            {personalizedGreeting(userProfile.displayName, debtProgressPercent)}
          </h1>
          <p className="text-sm text-milk-muted">USD · точность до центов</p>
        </div>

        <BalanceOverviewCard
          finance={finance.globalFinance}
          formatUsd={usd}
          hideAmounts={hideAmounts}
        />

        <PsychologySettingsPanel
          privacy={psychology.privacy}
          autopilot={psychology.autopilot}
          onPrivacyChange={psychology.updatePrivacy}
          onAutopilotChange={psychology.updateAutopilot}
          hideAmounts={hideAmounts}
        />

        <SyncPanel
          peerUrl={webSync.peerUrl}
          onPeerUrlChange={webSync.setPeerUrl}
          status={webSync.status}
          lastError={webSync.lastError}
          lastSyncedAt={webSync.lastSyncedAt}
          onSync={() => {
            void webSync.syncNow().then((ok) => {
              if (ok) showToast("Данные синхронизированы с телефоном");
            });
          }}
        />

        <div className="grid gap-3 sm:grid-cols-3">
          <StatCard
            label="Долги"
            value={usd(finance.dashboardTotals.totalDebtRemainingUsd)}
            icon={HandCoins}
            hint="Осталось получить"
          />
          <StatCard
            label="Свободно"
            value={usd(finance.dashboardTotals.availableBalanceUsd)}
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

      <main className="space-y-6">
        {hasOperations ? (
          <TransactionHistory transactions={finance.transactions} goals={goals} />
        ) : null}

        <div className="space-y-4">
          {(finance.typeFilter === "debt" || finance.typeFilter === "all") && activeDebts.length > 0 ? (
            <>
              <FreedomForecastCard debts={activeDebts} hideAmounts={hideAmounts} />
              <GamificationBanner state={psychology.gamification} />
              <div className="flex flex-wrap gap-1.5">
                <button type="button" onClick={() => setShowAdvisor(true)} className="pill-btn-secondary">
                  Советник
                </button>
                {psychology.autopilot.enabled ? (
                  <button type="button" onClick={applyAutopilot} className="pill-btn-primary">
                    Внести сейчас
                  </button>
                ) : null}
              </div>
            </>
          ) : null}

          <div className="space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h2 className="text-base font-medium text-milk-text">
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
                      className="pill-btn-primary inline-flex items-center gap-1.5"
                    >
                      <AppIcon icon={Target} size={14} className="text-white" />
                      Новая цель
                    </button>
                    <button
                      type="button"
                      onClick={() => setShowAddDebt(true)}
                      className="pill-btn-secondary inline-flex items-center gap-1.5"
                    >
                      <AppIcon icon={HandCoins} size={14} />
                      Новый долг
                    </button>
                  </>
                ) : undefined
              }
            />
          ) : (
            <div className="grid gap-3 lg:grid-cols-2">
              <AnimatePresence mode="popLayout">
                {finance.visibleItems.map((item) => (
                  <FinanceCard
                    key={item.id}
                    item={item}
                    hideAmounts={hideAmounts}
                    onAddPayment={openPayment}
                    onGoalTransfer={setTransferGoal}
                    onGoalExpense={() => setAmountAction("expense")}
                    onDelete={setPendingDelete}
                  />
                ))}
              </AnimatePresence>
            </div>
          )}
        </div>
      </main>

      <PaymentModal
        item={paymentItem}
        open={paymentItem !== null}
        initialAmountCents={paymentPrefillCents}
        onClose={() => {
          setPaymentItem(null);
          setPaymentPrefillCents(0);
        }}
        onSubmit={(id, amount) => {
          const item = paymentItem;
          const result = finance.processPayment(id, amount);
          if (result.ok) {
            if (item?.type === "DEBT") {
              const paymentCents = parseMoneyInput(amount);
              if (paymentCents) {
                psychology.recordDebtPayment(item, paymentCents);
              }
            }
            showToast("Операция сохранена");
          }
          return result;
        }}
      />

      <DebtAdvisorSheet
        open={showAdvisor}
        messages={psychology.advisorHistory}
        onPrompt={(promptId) => psychology.askAdvisor(promptId, finance.state.items)}
        onClose={() => setShowAdvisor(false)}
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

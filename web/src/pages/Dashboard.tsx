import { AnimatePresence } from "framer-motion";
import {
  ArrowDownLeft,
  ArrowLeft,
  ArrowUpRight,
  HandCoins,
  Inbox,
  PiggyBank,
  Search,
  Settings,
  Target,
  TrendingUp,
} from "lucide-react";
import { useCallback, useState } from "react";
import { personalizedGreeting } from "@money/shared";
import { OnboardingFlow } from "@/components/onboarding/OnboardingFlow";
import { FreedomForecastCard } from "@/components/debt/FreedomForecastCard";
import { FinanceCard } from "@/components/cards/FinanceCard";
import { GoalSectionSummary } from "@/components/cards/GoalSectionSummary";
import { BalanceOverviewCard } from "@/components/finance/BalanceOverviewCard";
import { TransactionHistory } from "@/components/finance/TransactionHistory";
import { AmountModal } from "@/components/modals/AmountModal";
import { ConfirmDeleteModal } from "@/components/modals/ConfirmDeleteModal";
import { DebtFormModal } from "@/components/modals/DebtFormModal";
import { GoalFormModal } from "@/components/modals/GoalFormModal";
import { PaymentModal } from "@/components/modals/PaymentModal";
import { AdvisorConversation } from "@/components/psychology/DebtAdvisorSheet";
import { GamificationBanner } from "@/components/psychology/GamificationBanner";
import { PsychologySettingsPanel } from "@/components/psychology/PsychologySettingsPanel";
import { AppTabBar, type AppTab } from "@/components/ui/AppTabBar";
import { EmptyState } from "@/components/ui/EmptyState";
import { ScoreRing } from "@/components/ui/ScoreRing";
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
  const [pendingDelete, setPendingDelete] = useState<FinancialItem | null>(null);
  const [tab, setTab] = useState<AppTab>("home");
  const [recordQuery, setRecordQuery] = useState("");
  const [activityQuery, setActivityQuery] = useState("");

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

  const recordQueryNormalized = recordQuery.trim().toLowerCase();
  const listedItems = recordQueryNormalized
    ? finance.visibleItems.filter((item) => item.name.toLowerCase().includes(recordQueryNormalized))
    : finance.visibleItems;
  const activityQueryNormalized = activityQuery.trim().toLowerCase();
  const visibleTransactions = activityQueryNormalized
    ? finance.transactions.filter((tx) => {
        const goalName = goals.find((item) => item.id === tx.goalId)?.name ?? "";
        return `${tx.note ?? ""} ${tx.category ?? ""} ${goalName}`
          .toLowerCase()
          .includes(activityQueryNormalized);
      })
    : finance.transactions;
  const debtProgressPercent =
    activeDebts.length > 0
      ? Math.min(
          100,
          Math.floor(
            (activeDebts.reduce((sum, d) => sum + d.amount, 0) /
              Math.max(
                1,
                activeDebts.reduce((sum, d) => sum + d.target, 0),
              )) *
              100,
          ),
        )
      : 0;
  const goalPoolCents =
    finance.globalFinance.totalAccumulatedCents + finance.globalFinance.totalRemainingToGoalCents;
  const goalProgressPercent =
    goalPoolCents > 0
      ? Math.round((finance.globalFinance.totalAccumulatedCents / goalPoolCents) * 100)
      : 0;
  const score = activeDebts.length > 0 ? debtProgressPercent : goalProgressPercent;

  return (
    <div className="mx-auto min-h-dvh w-full max-w-md px-4 pb-32 pt-6">
      {tab === "more" ? (
        <section className="space-y-4">
          <button
            type="button"
            onClick={() => setTab("home")}
            className="inline-flex items-center gap-2 text-sm font-semibold text-milk-accent"
          >
            <AppIcon icon={ArrowLeft} size={16} className="text-milk-accent" />
            Назад
          </button>
          <h1 className="text-2xl font-semibold tracking-tight">Настройки</h1>
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
        </section>
      ) : (
        <>
          <header className="mb-5 flex items-start justify-between gap-3">
            <div className="min-w-0">
              <p className="text-sm font-medium text-milk-muted">Финансовый контроль</p>
              <h1 className="text-2xl font-semibold tracking-tight text-milk-text">
                {personalizedGreeting(
                  userProfile.displayName,
                  debtProgressPercent,
                  navigator.language,
                )}
              </h1>
            </div>
            <button
              type="button"
              aria-label="Настройки"
              onClick={() => setTab("more")}
              className="flex size-11 shrink-0 items-center justify-center rounded-full bg-white shadow-[0_8px_20px_rgb(15_110_92_/_0.08)]"
            >
              <AppIcon icon={Settings} size={18} className="text-milk-accent" />
            </button>
          </header>

          <main className="space-y-5">
            {tab === "home" ? (
              <>
                <BalanceOverviewCard
                  finance={finance.globalFinance}
                  formatUsd={usd}
                  hideAmounts={hideAmounts}
                />

                <section className="space-y-3">
                  <h2 className="text-base font-semibold">Сводка</h2>
                  <div className="grid grid-cols-2 gap-3">
                    <StatCard
                      label="Долги"
                      value={usd(finance.dashboardTotals.totalDebtRemainingUsd)}
                      icon={HandCoins}
                      hint="Осталось получить"
                      tone="mint"
                    />
                    <StatCard
                      label="Свободно"
                      value={usd(finance.dashboardTotals.availableBalanceUsd)}
                      icon={TrendingUp}
                      hint="Для переводов"
                      tone="peach"
                    />
                    <StatCard
                      label="Активных"
                      value={String(finance.dashboardTotals.activeCount)}
                      icon={Target}
                      hint={`Завершено: ${finance.dashboardTotals.completedCount}`}
                      tone="lilac"
                    />
                    <StatCard
                      label="Накоплено"
                      value={money(finance.globalFinance.totalAccumulatedCents)}
                      icon={PiggyBank}
                      hint="На активных целях"
                      tone="sand"
                    />
                  </div>
                </section>

                <ScoreRing
                  value={score}
                  label={activeDebts.length > 0 ? "Погашение долгов" : "Накопления"}
                  caption={
                    activeDebts.length > 0
                      ? "Доля уже закрытой суммы долгов"
                      : "Доля накоплений от суммы целей"
                  }
                />

                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => setAmountAction("deposit")}
                    className="pill-btn-primary inline-flex flex-1 items-center justify-center gap-1.5"
                  >
                    <AppIcon icon={ArrowDownLeft} size={16} className="text-white" />
                    Пополнить
                  </button>
                  <button
                    type="button"
                    onClick={() => setAmountAction("expense")}
                    className="pill-btn-secondary inline-flex flex-1 items-center justify-center gap-1.5"
                  >
                    <AppIcon icon={ArrowUpRight} size={16} />
                    Расход
                  </button>
                </div>

                {activeDebts.length > 0 ? (
                  <>
                    <FreedomForecastCard debts={activeDebts} hideAmounts={hideAmounts} />
                    <GamificationBanner state={psychology.gamification} />
                    {psychology.autopilot.enabled ? (
                      <button type="button" onClick={applyAutopilot} className="pill-btn-primary w-full">
                        Внести сейчас
                      </button>
                    ) : null}
                  </>
                ) : null}
              </>
            ) : null}

            {tab === "activity" ? (
              <section className="space-y-3">
                <h2 className="text-base font-semibold">Операции</h2>
                <label className="flex items-center gap-2 rounded-full bg-white px-4 py-3 shadow-[0_8px_20px_rgb(15_110_92_/_0.06)]">
                  <AppIcon icon={Search} size={16} />
                  <input
                    value={activityQuery}
                    onChange={(event) => setActivityQuery(event.target.value)}
                    placeholder="Поиск операций"
                    className="w-full bg-transparent text-sm outline-none placeholder:text-milk-muted"
                  />
                </label>
                {visibleTransactions.length === 0 ? (
                  <EmptyState
                    icon={Inbox}
                    title={hasOperations ? "Ничего не найдено" : "Операций пока нет"}
                    description={
                      hasOperations
                        ? "Попробуйте другой запрос."
                        : "Пополните баланс или запишите расход — история появится здесь."
                    }
                  />
                ) : (
                  <TransactionHistory transactions={visibleTransactions} goals={goals} />
                )}
              </section>
            ) : null}

            {tab === "records" ? (
              <section className="space-y-4">
                <div className="space-y-3">
                  <h2 className="text-base font-semibold">Записи ({listedItems.length})</h2>
                  <label className="flex items-center gap-2 rounded-full bg-white px-4 py-3 shadow-[0_8px_20px_rgb(15_110_92_/_0.06)]">
                    <AppIcon icon={Search} size={16} />
                    <input
                      value={recordQuery}
                      onChange={(event) => setRecordQuery(event.target.value)}
                      placeholder="Поиск целей и долгов"
                      className="w-full bg-transparent text-sm outline-none placeholder:text-milk-muted"
                    />
                  </label>
                  <StatusFilterTabs value={finance.statusFilter} onChange={finance.setStatusFilter} />
                  <TypeFilterChips value={finance.typeFilter} onChange={finance.setTypeFilter} />
                </div>

                {finance.typeFilter === "goal" ? (
                  <GoalSectionSummary items={listedItems} hideAmounts={hideAmounts} />
                ) : null}

                {listedItems.length === 0 ? (
                  <EmptyState
                    icon={Inbox}
                    title={recordQueryNormalized ? "Ничего не найдено" : "Пока пусто"}
                    description={
                      recordQueryNormalized ? "Попробуйте другое название." : emptyDescription
                    }
                    action={
                      !recordQueryNormalized && finance.statusFilter !== "completed" ? (
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
                  <div className="grid gap-3">
                    <AnimatePresence mode="popLayout">
                      {listedItems.map((item) => (
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
              </section>
            ) : null}

            {tab === "advisor" ? (
              <AdvisorConversation
                messages={psychology.advisorHistory}
                onPrompt={(promptId) => psychology.askAdvisor(promptId, finance.state.items)}
              />
            ) : null}
          </main>
        </>
      )}

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

      <AppTabBar
        tab={tab}
        onTabChange={setTab}
        actions={[
          { label: "Пополнить", icon: ArrowDownLeft, onClick: () => setAmountAction("deposit") },
          { label: "Расход", icon: ArrowUpRight, onClick: () => setAmountAction("expense") },
          { label: "Новый долг", icon: HandCoins, onClick: () => setShowAddDebt(true) },
          { label: "Новая цель", icon: Target, onClick: () => setShowAddGoal(true) },
        ]}
      />
    </div>
  );
}

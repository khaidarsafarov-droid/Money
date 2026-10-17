import { AnimatePresence } from "framer-motion";
import {
  ArrowLeft,
  HandCoins,
  Inbox,
  PiggyBank,
  Search,
  Settings,
  Sparkles,
  Target,
} from "lucide-react";
import { useCallback, useState } from "react";
import { personalizedGreeting } from "@money/shared";
import { OnboardingFlow } from "@/components/onboarding/OnboardingFlow";
import { FreedomForecastCard } from "@/components/debt/FreedomForecastCard";
import { FinanceCard } from "@/components/cards/FinanceCard";
import { GoalSectionSummary } from "@/components/cards/GoalSectionSummary";
import { NotesPanel } from "@/components/notes/NotesPanel";
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
import { SyncPanel } from "@/components/sync/SyncPanel";
import { useToast } from "@/components/ui/Toast";
import { useUserProfile } from "@/hooks/useUserProfile";
import { useAppPsychology } from "@/hooks/useAppPsychology";
import { useFinanceManager } from "@/hooks/useFinanceManager";
import { useWebSync } from "@/hooks/useWebSync";
import { usePaymentDeepLink } from "@/hooks/usePaymentDeepLink";
import { formatMoney, parseMoneyInput } from "@/lib/money";
import { remainingAmount } from "@/lib/financeEngine";
import type { FinancialItem } from "@/types/finance";

export function DashboardPage() {
  const finance = useFinanceManager();
  const psychology = useAppPsychology();
  const { profile: userProfile } = useUserProfile();
  const { showToast } = useToast();
  const webSync = useWebSync(finance.replaceState, () => finance.state);
  const [paymentItem, setPaymentItem] = useState<FinancialItem | null>(null);
  const [paymentPrefillCents, setPaymentPrefillCents] = useState(0);
  const [transferGoal, setTransferGoal] = useState<FinancialItem | null>(null);
  const [expenseGoal, setExpenseGoal] = useState<FinancialItem | null>(null);
  const [showAddGoal, setShowAddGoal] = useState(false);
  const [showAddDebt, setShowAddDebt] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<FinancialItem | null>(null);
  const [tab, setTab] = useState<AppTab>("home");
  const [recordQuery, setRecordQuery] = useState("");
  const [showAdvisor, setShowAdvisor] = useState(false);

  const hideAmounts = psychology.privacy.hideAmounts;
  const money = useCallback(
    (cents: number) => formatMoney(cents, { hidden: hideAmounts }),
    [hideAmounts],
  );

  const activeDebts = finance.state.items.filter(
    (item) => item.type === "DEBT" && item.status !== "COMPLETED" && !item.deleted,
  );
  const homeGoals = finance.state.items.filter(
    (item) => item.type === "GOAL" && item.status !== "COMPLETED" && !item.deleted,
  ).slice(0, 4);
  const homeDebts = activeDebts.slice(0, 4);

  const openPayment = useCallback((item: FinancialItem, prefillCents = 0) => {
    setPaymentPrefillCents(prefillCents);
    setPaymentItem(item);
  }, []);

  usePaymentDeepLink(finance.state.items, openPayment);

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
      ? "Завершённые записи появятся здесь."
      : tab === "debts"
        ? "Добавьте первый долг, чтобы отслеживать возвраты."
        : "Создайте цель и начните копить на важную задачу.";

  if (!userProfile.onboardingCompleted) {
    return <OnboardingFlow />;
  }

  const recordQueryNormalized = recordQuery.trim().toLowerCase();
  const listedItems = finance.visibleItems.filter((item) => {
    const onGoals = tab === "goals" && item.type === "GOAL";
    const onDebts = tab === "debts" && item.type === "DEBT";
    if (!onGoals && !onDebts) return false;
    if (!recordQueryNormalized) return true;
    return item.name.toLowerCase().includes(recordQueryNormalized);
  });
  const debtPaidCents = activeDebts.reduce((sum, debt) => sum + debt.amount, 0);
  const debtTargetCents = activeDebts.reduce((sum, debt) => sum + debt.target, 0);
  const debtProgressPercent =
    debtTargetCents > 0
      ? Math.min(100, Math.floor((debtPaidCents * 100) / debtTargetCents))
      : 0;
  const goalPoolCents =
    finance.globalFinance.totalAccumulatedCents + finance.globalFinance.totalRemainingToGoalCents;
  const goalProgressPercent =
    goalPoolCents > 0
      ? Math.min(100, Math.floor((finance.globalFinance.totalAccumulatedCents * 100) / goalPoolCents))
      : 0;
  const score = activeDebts.length > 0 ? debtProgressPercent : goalProgressPercent;
  const debtRemainingCents = activeDebts.reduce((sum, debt) => sum + remainingAmount(debt), 0);
  const ringRemaining = activeDebts.length > 0 ? money(debtRemainingCents) : money(finance.globalFinance.totalRemainingToGoalCents);
  const ringCaption =
    activeDebts.length > 0
      ? `Закрыто ${score}% · осталось ${ringRemaining}`
      : `Накоплено ${score}% · осталось ${ringRemaining}`;

  return (
    <div className="mx-auto min-h-dvh w-full max-w-md px-4 pb-32 pt-6">
      {tab === "more" ? (
        <section className="space-y-4">
          <button
            type="button"
            onClick={() => setTab("home")}
            className="inline-flex items-center gap-2 text-sm font-semibold text-primary"
          >
            <AppIcon icon={ArrowLeft} size={16} className="text-primary" />
            Назад
          </button>
          <h1 className="text-2xl font-semibold tracking-tight">Настройки</h1>
          {showAdvisor ? (
            <AdvisorConversation
              messages={psychology.advisorHistory}
              onPrompt={(promptId) => psychology.askAdvisor(promptId, finance.state.items)}
            />
          ) : (
            <PsychologySettingsPanel
              privacy={psychology.privacy}
              autopilot={psychology.autopilot}
              onPrivacyChange={psychology.updatePrivacy}
              onAutopilotChange={psychology.updateAutopilot}
              hideAmounts={hideAmounts}
            />
          )}
          <button
            type="button"
            onClick={() => setShowAdvisor((value) => !value)}
            className="pill-btn-secondary inline-flex items-center gap-1.5"
          >
            <AppIcon icon={Sparkles} size={14} />
            {showAdvisor ? "К настройкам" : "Советник"}
          </button>
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
              <p className="text-sm font-medium text-on-surface-variant">Финансовый контроль</p>
              <h1 className="text-2xl font-semibold tracking-tight text-on-surface">
                {personalizedGreeting(userProfile.displayName, score, "ru")}
              </h1>
            </div>
            <button
              type="button"
              aria-label="Настройки"
              onClick={() => setTab("more")}
              className="flex size-12 shrink-0 items-center justify-center rounded-full bg-surface-container-lowest shadow-[0_8px_20px_rgb(15_110_92_/_0.08)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
            >
              <AppIcon icon={Settings} size={18} className="text-primary" />
            </button>
          </header>

          <main className="space-y-5">
            {tab === "home" ? (
              <>
                <section className="space-y-3">
                  <h2 className="text-base font-semibold">Сводка</h2>
                  <div className="grid grid-cols-2 gap-3">
                    <StatCard
                      label="Я должен"
                      value={money(debtRemainingCents)}
                      icon={HandCoins}
                      hint="Осталось закрыть"
                      tone="peach"
                    />
                    <StatCard
                      label="Накоплено"
                      value={money(finance.globalFinance.totalAccumulatedCents)}
                      icon={PiggyBank}
                      hint="На денежных целях"
                      tone="lilac"
                    />
                    <StatCard
                      label="До целей"
                      value={money(finance.globalFinance.totalRemainingToGoalCents)}
                      icon={Target}
                      hint="Сколько ещё собрать"
                      tone="sand"
                    />
                  </div>
                </section>

                <ScoreRing
                  value={score}
                  label="Прогресс"
                  caption={ringCaption}
                />

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

                {homeGoals.length === 0 && homeDebts.length === 0 ? (
                  <EmptyState
                    icon={Inbox}
                    title="Пока только цели и долги"
                    description="Создайте цель или добавьте долг — всё в одном месте."
                  />
                ) : null}
                {homeGoals.length > 0 ? (
                  <PreviewList
                    title="Цели"
                    items={homeGoals}
                    hideAmounts={hideAmounts}
                    onOpen={() => setTab("goals")}
                    onAddPayment={openPayment}
                    onGoalTransfer={setTransferGoal}
                    onGoalExpense={(item) => setExpenseGoal(item)}
                    onDelete={setPendingDelete}
                  />
                ) : null}
                {homeDebts.length > 0 ? (
                  <PreviewList
                    title="Долги"
                    items={homeDebts}
                    hideAmounts={hideAmounts}
                    onOpen={() => setTab("debts")}
                    onAddPayment={openPayment}
                    onGoalTransfer={setTransferGoal}
                    onGoalExpense={(item) => setExpenseGoal(item)}
                    onDelete={setPendingDelete}
                  />
                ) : null}
              </>
            ) : null}

            {tab === "goals" || tab === "debts" ? (
              <section className="space-y-4">
                <div className="space-y-3">
                  <h2 className="text-base font-semibold">
                    {tab === "goals" ? "Цели" : "Долги"} ({listedItems.length})
                  </h2>
                  <label className="block space-y-1.5">
                    <span className="text-sm text-on-surface-variant">
                      {tab === "goals" ? "Поиск целей" : "Поиск долгов"}
                    </span>
                    <span className="flex min-h-12 items-center gap-2 rounded-full bg-surface-container-lowest px-4 shadow-[0_8px_20px_rgb(15_110_92_/_0.06)]">
                      <AppIcon icon={Search} size={16} />
                      <input
                        name="q"
                        type="search"
                        autoComplete="off"
                        value={recordQuery}
                        onChange={(event) => setRecordQuery(event.target.value)}
                        placeholder={tab === "goals" ? "Название цели" : "Имя или название"}
                        aria-describedby={recordQueryNormalized && listedItems.length === 0 ? "search-empty" : undefined}
                        className="w-full bg-transparent py-3 text-sm text-on-surface placeholder:text-on-surface-variant focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                      />
                    </span>
                  </label>
                  <StatusFilterTabs value={finance.statusFilter} onChange={finance.setStatusFilter} />
                </div>

                {tab === "goals" ? (
                  <GoalSectionSummary items={listedItems} hideAmounts={hideAmounts} />
                ) : null}

                {listedItems.length === 0 ? (
                  <div id={recordQueryNormalized ? "search-empty" : undefined}>
                  <EmptyState
                    icon={Inbox}
                    title={recordQueryNormalized ? "Ничего не найдено" : "Пока пусто"}
                    description={
                      recordQueryNormalized ? "Попробуйте другое название." : emptyDescription
                    }
                    action={
                      !recordQueryNormalized && finance.statusFilter !== "completed" ? (
                        tab === "goals" ? (
                          <button
                            type="button"
                            onClick={() => setShowAddGoal(true)}
                            className="pill-btn-primary inline-flex items-center gap-1.5"
                          >
                            <AppIcon icon={Target} size={14} className="text-on-primary" />
                            Новая цель
                          </button>
                        ) : (
                          <button
                            type="button"
                            onClick={() => setShowAddDebt(true)}
                            className="pill-btn-secondary inline-flex items-center gap-1.5"
                          >
                            <AppIcon icon={HandCoins} size={14} />
                            Новый долг
                          </button>
                        )
                      ) : undefined
                    }
                  />
                  </div>
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
                          onGoalExpense={() => setExpenseGoal(item)}
                          onDelete={setPendingDelete}
                        />
                      ))}
                    </AnimatePresence>
                  </div>
                )}
              </section>
            ) : null}

            {tab === "notes" ? <NotesPanel hideAmounts={hideAmounts} /> : null}
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
        hint="Сумма добавится к этой цели."
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
        open={expenseGoal !== null}
        title={expenseGoal ? `Расход — ${expenseGoal.name}` : "Расход с цели"}
        hint="Сумма спишется только с этой цели."
        submitLabel="Списать"
        onClose={() => setExpenseGoal(null)}
        onSubmit={(amount) => {
          if (!expenseGoal) return { ok: false, error: "Цель не выбрана" };
          const result = finance.addExpense(amount, "Общий", expenseGoal.id);
          if (result.ok) showToast("Расход с цели сохранён");
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
          { label: "Новая цель", icon: Target, onClick: () => { setTab("goals"); setShowAddGoal(true); } },
          { label: "Новый долг", icon: HandCoins, onClick: () => { setTab("debts"); setShowAddDebt(true); } },
          { label: "Новая заметка", icon: Sparkles, onClick: () => setTab("notes") },
        ]}
      />
    </div>
  );
}

function PreviewList({
  title,
  items,
  hideAmounts,
  onOpen,
  onAddPayment,
  onGoalTransfer,
  onGoalExpense,
  onDelete,
}: {
  title: string;
  items: FinancialItem[];
  hideAmounts: boolean;
  onOpen: () => void;
  onAddPayment: (item: FinancialItem, prefillCents?: number) => void;
  onGoalTransfer: (item: FinancialItem) => void;
  onGoalExpense: (item: FinancialItem) => void;
  onDelete: (item: FinancialItem) => void;
}) {
  return (
    <section className="space-y-3">
      <button
        type="button"
        onClick={onOpen}
        className="inline-flex min-h-12 items-center rounded-full bg-surface-container-lowest px-4 text-base font-semibold text-on-surface focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
      >
        {title}
      </button>
      <div className="grid gap-3">
        {items.map((item) => (
          <FinanceCard
            key={item.id}
            item={item}
            hideAmounts={hideAmounts}
            onAddPayment={onAddPayment}
            onGoalTransfer={onGoalTransfer}
            onGoalExpense={() => onGoalExpense(item)}
            onDelete={onDelete}
          />
        ))}
      </div>
    </section>
  );
}

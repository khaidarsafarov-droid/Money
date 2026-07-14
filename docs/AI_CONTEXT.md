# Money / Финансовый Контроль — AI Context Briefing

> **Purpose:** Paste this document (or upload `Money-source-for-AI.zip`) into AI Cloud so the assistant understands the full project.
> **Repo:** `khaidarsafarov-droid/Money` | **Branch:** `master`
> **Last updated:** July 2026

---

## 1. What is this app?

**Финансовый Контроль** (RU) / **Financial Control** (EN) is an **offline-first personal finance app** focused on:

- **Debts** — money owed to you (`CREDITOR`) and money you owe (`DEBTOR`)
- **Savings goals** — named targets with progress derived from transactions
- **Global balance** — available pool for transfers and expenses
- **Psychology layer** — gamification, debt advisor (template-based, no LLM), autopilot micro-payments, psychotype quiz, privacy masking

It is **not** a generic bank app. The differentiator is reducing financial stress through psychology-aware UX, not just bookkeeping.

**Currency:** USD only. **Precision:** integer cents everywhere (`Long` on Android, `number` on web).

---

## 2. Tech stack

| Layer | Stack |
|-------|-------|
| **Android (primary)** | Kotlin, Jetpack Compose, Material 3, Room DB v13, DataStore, WorkManager, Glance widgets, Ktor WebSocket, Android NSD |
| **Web companion** | React 19, TypeScript, Vite 7, Tailwind CSS 4, Framer Motion, Lucide icons |
| **Shared logic** | `@money/shared` — TypeScript package with finance engines, sync contract, psychology |
| **Server (legacy)** | Express 5 — dev REST API only, **not used in P2P sync** |
| **Monorepo** | npm workspaces (`packages/shared`, `web`) + Gradle (`:app`) |

**Package ID:** `com.example.money` | **Version:** 1.1 (versionCode 2)

---

## 3. Repository layout

```
Money/
├── app/                          # Android app (main product)
│   └── src/main/java/com/example/money/
│       ├── MainActivity.kt       # Entry point
│       ├── MoneyApplication.kt   # DI hub, starts sync/widget/voice
│       ├── data/                 # Room, DataStore, sync, repositories
│       ├── domain/               # Models, engines, repository interfaces
│       ├── ui/                   # Compose screens & components
│       ├── voice/                # TTS daily reports
│       └── widget/               # Glance home-screen widget
├── packages/shared/              # @money/shared — cross-platform TS logic
├── web/                          # React dashboard companion
├── server/                       # Legacy Express API (ignore for sync)
├── docs/PSYCHOLOGY_ROADMAP.md    # Psychology feature phases
├── ARCHITECTURE.md               # Technical architecture (RU)
└── docs/AI_CONTEXT.md            # This file
```

---

## 4. Core data model

### Unified mental model

```
FinanceState {
  currency: "USD"
  items: FinancialItem[]      // GOAL | DEBT
  transactions: Transaction[] // DEPOSIT | GOAL_TRANSFER | EXPENSE
}
```

### Android domain (`domain/model/`)

| Model | Purpose |
|-------|---------|
| `Debtor` | Debt record with cents, type, status, payment history, contact/calendar metadata |
| `SavingsGoal` | Named goal: target/current cents, optional deadline |
| `Transfer` | Deposit or goal transfer (`isConfirmed` flag) |
| `Expense` | Spending record, may link to debt payment |
| `DebtPayment` | Single payment against a debtor |
| `BudgetSummary` / `GlobalFinanceSummary` | Aggregated balances |

### Room tables (`money.db` v13)

| Table | Entity |
|-------|--------|
| `debtor` | `DebtorEntity` + `DebtPaymentEntity` |
| `savings_goal` | `SavingsGoalEntity` |
| `transfers` | `TransferEntity` |
| `expenses` | `ExpenseEntity` |
| `luck_diary_entry` | `LuckDiaryEntryEntity` |
| `sync_queue` | Pending P2P mutations |

**DataStore (non-Room):** user profile, privacy, gamification, autopilot, voice settings, advisor chat history.

### Key invariant

> Goal `currentAmount` is **derived from transactions**, not stored independently.
> Never compute balances in UI — use `GlobalFinanceEngine` / `globalFinanceEngine.ts`.

### Balance formulas (both platforms)

```
globalBalance     = deposits − expenses
availableBalance  = deposits − goalTransfers − expenses
totalAccumulated  = Σ accumulated on active goals
remainingToGoal   = Σ (target − accumulated) on active goals
```

---

## 5. Screens & navigation

### Android (`MoneyNavHost.kt`)

State-machine navigation (not Navigation Compose routes):

```
!onboardingCompleted  → OnboardingScreen
showGoalHub           → GoalScreen (psychology hub)
showPrivacySettings   → PrivacySettingsScreen
showAutopilotSettings → AutopilotSettingsScreen
showCalendarSettings  → CalendarSettingsScreen
showVoiceSettings     → VoiceSettingsScreen
showAppLanguage       → AppLanguageScreen
else                  → HomeScreen
```

**HomeScreen tabs:**
- `Debts` — debt management with sub-tabs: Owes Me / I Owe / Freedom / Calendar
- `BrotherFinances` (Operations) — deposits, expenses, transfers

**Settings drawer:** grouped menu (Goals / Settings / Tools) with icons.

**Tablet (≥600dp):** `TabletHomeLayout` — NavigationRail + list + detail pane.

### Web (`Dashboard.tsx`)

Single-page app:
1. Onboarding gate
2. Balance overview (progressive disclosure)
3. Filter tabs: status + type (goal/debt)
4. Finance cards, freedom forecast, gamification banner
5. Sync panel (manual WebSocket URL to Android peer)
6. FAB modals for add goal/debt/payment/deposit/expense

---

## 6. Business logic engines

### `@money/shared` (canonical TypeScript)

| Module | File | Purpose |
|--------|------|---------|
| Finance types | `finance.ts` | `FinanceState`, `Transaction`, filters |
| Sync | `sync.ts`, `syncEngine.ts`, `syncPeerSession.ts` | LWW merge, WebSocket protocol |
| Debt freedom | `debtFreedomEngine.ts` | Milestones, freedom date projection |
| Debt autopilot | `debtAutopilot.ts` | Snowball/avalanche, micro-payments |
| Debt gamification | `debtGamification.ts` | Streaks, badges, win messages |
| Debt advisor | `debtAdvisorTemplates.ts` | Template replies (no LLM) |
| Goal hub | `goalHubEngine.ts` | Payoff plan integration |
| Psychotype | `psychotype.ts` | 5-type financial personality quiz |
| User profile | `userProfile.ts` | Onboarding, greetings |
| Luck diary | `luckDiary.ts` | Monthly luck tracking |
| Income ideas | `incomeIdeas.ts` | Side-income resource links |

**Export barrel:** `packages/shared/src/index.ts`

### Android mirrors (`domain/util/`)

`GlobalFinanceEngine.kt`, `DebtFreedomEngine.kt`, `DebtPsychologyEngines.kt`, `GoalPsychologyEngines.kt`, `SyncEngine.kt`, etc.

**Rule:** Add psychology logic to `@money/shared` first, then mirror to Kotlin if needed.

---

## 7. Sync architecture

### Android P2P (primary path)

```
Mutation → SyncQueueRecorder → sync_queue
         → SyncService (NSD _money._tcp + Ktor WebSocket /sync)
         → Exchange FinanceSnapshot JSON
         → mergeSymmetric (LWW by syncUpdatedAt)
         → FinanceSnapshotApplier → Room
```

**Key files:**
- `data/sync/SyncService.kt`
- `data/sync/NsdPeerDiscovery.kt`
- `domain/sync/SyncEngine.kt`
- `packages/shared/src/syncPeerSession.ts`

### Web sync

- User enters Android peer WebSocket URL manually
- `useWebSync.ts` → `runSyncAsInitiator` from `@money/shared`
- Web does **not** run NSD server

### Legacy server (ignore for sync)

- `server/src/index.ts` — REST CRUD, separate data model, not connected to P2P

### Known sync gaps

- Android uses `Long` auto-increment IDs; web uses string UUIDs
- Android: separate `transfers[]` + `expenses[]`; web: unified `transactions[]`
- `SYNC_SCHEMA_VERSION = 2`

---

## 8. Psychology & gamification

Implemented per `docs/PSYCHOLOGY_ROADMAP.md`:

| Feature | Android | Web | Shared |
|---------|---------|-----|--------|
| Debt freedom forecast | `DebtFreedomTab` | `FreedomForecastCard` | `debtFreedomEngine.ts` |
| Privacy (hide amounts) | `PrivacySettingsScreen` | `PsychologySettingsPanel` | `appPreferences.ts` |
| Debt advisor | `DebtAdvisorBottomSheet` | `DebtAdvisorSheet` | `debtAdvisorTemplates.ts` |
| Gamification | `GamificationBanner` | `GamificationBanner` | `debtGamification.ts` |
| Autopilot micro-payments | `MicroPaymentWorker` | Dashboard button | `debtAutopilot.ts` |
| Psychotype test | `PsychotypeTestDialog` | Onboarding | `psychotype.ts` |
| Celebration on payoff | `DebtCelebrationDialog` | — | — |
| Voice TTS reports | `VoiceReportComposer` | — | advisor templates |

---

## 9. Design system ("Milk-Rich" Material 3)

### Android (`ui/theme/`)

| Token | Value |
|-------|-------|
| Background | `#F5F0E8` CreamBackground |
| Primary gold | `#C9A24D` GoldPrimary |
| Text | `#3E2C18` BrownText |
| Positive | `#4A7C59` FinancePositive |
| Negative | `#B35D5D` FinanceNegative |
| Typography | 24/18/14 display/heading/body |
| Spacing | `MoneySpacing` (4–32dp, 44dp touch targets) |

**Components:** `MoneyButton`, `MoneyDialog`, `StatusBadge`, `DebtorListItem`, `GoalListItem`, `EmptyStateMessage`

### Web (`web/src/index.css`)

Tailwind `@theme`: `milk-bg`, `milk-text`, `finance-positive/negative`, `.text-display`, `.pill-btn-primary`

---

## 10. ViewModels & repositories

| ViewModel | Responsibility |
|-----------|----------------|
| `DebtorsViewModel` | Debt CRUD, payments, filters, advisor, gamification, calendar |
| `BudgetViewModel` | Deposits, expenses, goal transfers |
| `GoalViewModel` | Single goal CRUD |
| `GoalHubViewModel` | Psychology hub: psychotype, challenges, luck diary |
| `OnboardingViewModel` | Multi-step profile setup |

| Repository | Data sources |
|------------|--------------|
| `DebtorRepositoryImpl` | debtor + payment + transfer + expense DAOs |
| `GoalRepositoryImpl` | savings_goal + transfer DAOs (derives currentAmount) |
| `BudgetRepositoryImpl` | transfer + expense + savings_goal DAOs |

**DI:** Manual factories via `MoneyApplication` singleton (no Hilt/Koin).

**Web:** `useFinanceManager` hook wraps `financeStore.ts` + `globalFinanceEngine.ts`.

---

## 11. i18n

| Platform | Approach |
|----------|----------|
| Android | `values/strings.xml` (RU default), `values-en/`, `values-ru/` (~550 strings) |
| Web | Inline Russian in components (no i18n framework) |
| Shared | Russian labels in psychology engines |
| Runtime | `AppLanguageManager` — System / RU / EN |

---

## 12. Testing

```bash
npm install && npm test              # shared + web (Vitest)
./gradlew :app:testDebugUnitTest     # Android (JUnit + Robolectric)
```

**Coverage:** sync engine, finance formulas, debt freedom, gamification, psychotype, goal progress, input validators, voice reports.

---

## 13. Critical files quick reference

| Concern | Path |
|---------|------|
| App entry | `app/.../MainActivity.kt` |
| DI / lifecycle | `app/.../MoneyApplication.kt` |
| Navigation | `app/.../ui/navigation/MoneyNavHost.kt` |
| Main UI | `app/.../ui/home/HomeScreen.kt` |
| Room schema | `app/.../data/local/AppDatabase.kt` |
| P2P sync | `app/.../data/sync/SyncService.kt` |
| Shared contract | `packages/shared/src/sync.ts`, `index.ts` |
| Web dashboard | `web/src/pages/Dashboard.tsx` |
| Web state | `web/src/lib/financeStore.ts` (key: `truckerload-finance-v5`) |
| Architecture | `ARCHITECTURE.md` |
| Psychology roadmap | `docs/PSYCHOLOGY_ROADMAP.md` |

---

## 14. Rules for AI assistants

When giving advice or suggesting changes:

1. **Never compute balances in UI** — use engines only
2. **All money in cents** — USD only
3. **Psychology logic** → `@money/shared` first, then Kotlin mirror
4. **Sync mutations** on Android → must go through `SyncQueueRecorder`
5. **Room migrations** required for any entity schema change (currently v13)
6. **`server/`** is legacy — do not confuse with P2P sync
7. **Android is the primary app** — web is a thinner companion
8. **i18n:** Android uses `strings.xml`; web is mostly hardcoded Russian
9. **No inline imports** — keep imports at top of file
10. **Minimize scope** — match existing conventions, don't over-engineer

---

## 15. Recent state (master, July 2026)

Latest merge: **UI/UX audit** — design system tokens, simplified debt/goal cards with overflow menus, dashboard progressive disclosure, grouped settings drawer, empty states with CTAs, web styling aligned with Android.

---

## 16. How to use with AI Cloud

**Option A — Upload archive:**
Upload `Money-source-for-AI.zip` (source only, no `node_modules`/build artifacts, ~1.3 MB).

**Option B — Paste this document:**
Copy this entire `docs/AI_CONTEXT.md` as system/context prompt.

**Option C — Both:**
Upload zip + paste this briefing for best results.

**Suggested prompt for AI Cloud:**

```
You are helping with the Money app (Финансовый Контроль) — an offline-first
personal finance tracker for debts, savings goals, and global balance, with a
psychology/gamification layer. Stack: Android (Kotlin/Compose/Room) + React web
companion + @money/shared TypeScript package. Read the attached AI_CONTEXT.md
and source files. Follow the rules in section 14. Currency is USD cents only.
```

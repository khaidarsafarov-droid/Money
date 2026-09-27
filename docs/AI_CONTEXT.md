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
| **Android (primary)** | Kotlin, Jetpack Compose, Material 3, Room DB v22, DataStore, WorkManager, Glance widgets, Ktor WebSocket, Android NSD |
| **Web companion** | React 19, TypeScript, Vite 7, Tailwind CSS 4, Framer Motion, Lucide icons |
| **Shared logic** | `@money/shared` — TypeScript package with finance engines, sync contract, psychology |
| **Server (legacy)** | Express 5 — dev REST API only, **not used in P2P sync** |
| **Monorepo** | npm workspaces (`packages/shared`, `web`) + Gradle (`:app`) |

**Package ID:** `com.example.money` (owner must decide permanent Play ID) | **Version:** 1.2.0 (versionCode 3)

---

## 3. Repository layout

```
Money/
├── app/                          # Android app (main product)
│   └── src/main/java/com/example/money/
│       ├── MainActivity.kt       # Entry point
│       ├── MoneyApplication.kt   # DI hub, starts sync/widget/voice
│       ├── data/                 # Room, DataStore, sync, repositories
│       │   └── backup/           # Encrypted local/optional Drive backup + reset
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

### Room tables (`money.db` v22)

| Table | Entity |
|-------|--------|
| `debtor` | `DebtorEntity` |
| `debt_payment` | `DebtPaymentEntity` |
| `savings_goal` | `SavingsGoalEntity` |
| `transfers` | `TransferEntity` |
| `expenses` | `ExpenseEntity` |
| `luck_diary_entry` | `LuckDiaryEntryEntity` |
| `sync_queue` | Pending P2P mutations |
| `sync_tombstone` | Durable P2P deletion records |

**DataStore (non-Room):** user/profile/onboarding, widget snapshot, privacy,
gamification, autopilot, voice, calendar/personal-debt settings, advisor history,
backup device state, and sync device ID. The automatic-backup password is stored
separately as Android-Keystore-wrapped SharedPreferences.

### Key invariant

> Goal `currentAmount` is **derived from transactions**, not stored independently.
> Never compute balances in UI — use `GlobalFinanceEngine` / `globalFinanceEngine.ts`.

### Balance formulas (both platforms)

```
globalBalance     = deposits − free expenses
availableBalance  = deposits − free expenses
goal balance      = goal transfers − expenses taken from that goal
totalAccumulated  = Σ goal balance on active goals
remainingToGoal   = Σ (target − accumulated) on active goals
```

---

## 5. Screens & navigation

### Android (`MoneyNavHost.kt`)

The onboarding-completed DataStore flow gates a Navigation Compose host:

```
!onboardingCompleted → OnboardingScreen
onboardingCompleted  → NavHost(HOME)
HOME routes          → goal/settings/backup screens
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
- `SYNC_SCHEMA_VERSION = 3` on Kotlin and `@money/shared`; v2 snapshots are
  upgraded by the codecs.

---

## 8. Backup, restore, and local reset

- `BackupExporter` creates portable bundle format v1 from Room, portable
  preferences, and the optional vision image under `SyncWriteGuard`.
- `PasswordBackupCrypto` uses PBKDF2-HMAC-SHA256 plus AES-256-GCM. The password is
  never included in the backup and cannot be recovered.
- Local files use Android's Storage Access Framework.
- Google Drive is optional and limited to `drive.appdata` / `appDataFolder`.
  Automatic backup uses WorkManager and retains the newest five app backups.
- Restore validates before mutation, replaces Room data and portable settings,
  clears sync queue/tombstones, rolls back on apply failure, then reconciles
  reminders, schedules, widget, and calendar.
- `DefaultLocalDataResetter` clears Room business/sync/tombstone data, local
  preferences, sync device ID, image and wrapped credential, WorkManager work,
  notifications, and calendar links where permitted; it stops P2P first. It
  never deletes remote Drive backups.
- Setup and semantics are documented in `docs/BACKUP_RESTORE.md`.

---

## 9. Psychology & gamification

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

## 10. Design system ("Milk-Rich" Material 3)

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

## 11. ViewModels & repositories

| ViewModel | Responsibility |
|-----------|----------------|
| `DebtorsViewModel` | Debt CRUD, payments, filters, advisor, gamification, calendar |
| `BudgetViewModel` | Deposits, expenses, goal transfers |
| `GoalViewModel` | Single goal CRUD |
| `GoalHubViewModel` | Psychology hub: psychotype, challenges, luck diary |
| `OnboardingViewModel` | Multi-step profile setup |
| `BackupSettingsViewModel` | Local/Drive backup, restore, retention and safe local reset |

| Repository | Data sources |
|------------|--------------|
| `DebtorRepositoryImpl` | debtor + payment + transfer + expense DAOs |
| `GoalRepositoryImpl` | savings_goal + transfer DAOs (derives currentAmount) |
| `BudgetRepositoryImpl` | transfer + expense + savings_goal DAOs |
| `BackupRepositoryImpl` | encrypted portable export/import; remote storage remains a separate provider |

**DI:** Manual factories via `MoneyApplication` singleton (no Hilt/Koin).

**Web:** `useFinanceManager` hook wraps `financeStore.ts` + `globalFinanceEngine.ts`.

---

## 12. i18n

| Platform | Approach |
|----------|----------|
| Android | `values/strings.xml` (RU default), `values-en/`, `values-ru/` (~550 strings) |
| Web | Inline Russian in components (no i18n framework) |
| Shared | Russian labels in psychology engines |
| Runtime | `AppLanguageManager` — System / RU / EN |

---

## 13. Testing

```bash
npm ci
npm test
npm run build --workspace=money-web
ANDROID_HOME=$HOME/android-sdk ./gradlew :app:testDebugUnitTest :app:assembleDebug
ANDROID_HOME=$HOME/android-sdk ./gradlew :app:bundleRelease
```

**Current coverage:** Room migrations; sync v3/tombstones; repositories; finance
and psychology engines; voice/reminder/calendar behavior; backup codec, crypto,
round-trip, rollback, Google transport, automatic worker, complete local reset,
and Backup Settings ViewModel state; shared TS engines and web logic. CI runs
Android JVM tests + debug assembly and Node shared/web tests + production web
build.

---

## 14. Critical files quick reference

| Concern | Path |
|---------|------|
| App entry | `app/.../MainActivity.kt` |
| DI / lifecycle | `app/.../MoneyApplication.kt` |
| Navigation | `app/.../ui/navigation/MoneyNavHost.kt` |
| Main UI | `app/.../ui/home/HomeScreen.kt` |
| Room schema | `app/.../data/local/AppDatabase.kt` |
| P2P sync | `app/.../data/sync/SyncService.kt` |
| Backup/reset | `app/.../data/backup/`, `app/.../ui/settings/BackupSettings*` |
| Shared contract | `packages/shared/src/sync.ts`, `index.ts` |
| Web dashboard | `web/src/pages/Dashboard.tsx` |
| Web state | `web/src/lib/financeStore.ts` (key: `truckerload-finance-v5`) |
| Architecture | `ARCHITECTURE.md` |
| Psychology roadmap | `docs/PSYCHOLOGY_ROADMAP.md` |

---

## 15. Rules for AI assistants

When giving advice or suggesting changes:

1. **Never compute balances in UI** — use engines only
2. **All money in cents** — USD only
3. **Psychology logic** → `@money/shared` first, then Kotlin mirror
4. **Sync mutations** on Android → must go through `SyncQueueRecorder`
5. **Room migrations** required for any entity schema change (currently v22)
6. **`server/`** is legacy — do not confuse with P2P sync
7. **Android is the primary app** — web is a thinner companion
8. **i18n:** Android uses `strings.xml`; web is mostly hardcoded Russian
9. **No inline imports** — keep imports at top of file
10. **Minimize scope** — match existing conventions, don't over-engineer

---

## 16. Recent state (July 2026)

Room v22 and sync schema v3 are current. The backup layer supports encrypted
local files, optional Google Drive `appDataFolder`, automatic retention, atomic
Room restore with rollback, and guarded deletion of all local app data. Release
CI and Play/privacy/backup operator documents live in `.github/workflows/` and
`docs/`.

---

## 17. How to use with AI Cloud

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
and source files. Follow the rules in section 15. Currency is USD cents only.
```

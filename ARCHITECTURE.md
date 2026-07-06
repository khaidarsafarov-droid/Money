# Project Money — Architecture

Monolithic Local Engine: одна финансовая модель, offline-first, P2P-синхронизация между Android-устройствами в локальной Wi‑Fi сети, web-дашборд как companion UI.

## Принципы

| Принцип | Реализация |
|---------|------------|
| Валюта | Только **USD** |
| Точность | Все суммы — **целые центы** (`number` / `Long`) |
| Источник правды | **Транзакции** (DEPOSIT, GOAL_TRANSFER, EXPENSE) для целей и глобального баланса |
| Offline-first | Room (Android) + localStorage v5 (web) |
| Sync | LWW по `syncUpdatedAt`, tombstone `deleted` |
| UI | Milk-Rich M3: фон `#F9F7F2`, positive `#4A7C59`, negative `#B35D5D` |

## Структура репозитория

```
Money/
├── packages/shared/          # Единый TS-контракт (Transaction, FinanceSnapshot, merge)
├── web/                      # React 19 dashboard
├── app/                      # Android (Compose, Room, Glance, TTS, P2P)
├── server/                   # Legacy/dev API (не участвует в P2P sync)
└── ARCHITECTURE.md
```

## Phase A — Data Layer (ядро)

### Web

| Модуль | Назначение |
|--------|------------|
| `@money/shared` | `Transaction`, `FinanceState`, `FinanceSnapshot`, константы |
| `web/src/lib/globalFinanceEngine.ts` | Расчёт баланса из транзакций |
| `web/src/hooks/useFinanceManager.ts` | UI-слой над engine + store |
| `web/src/lib/financeStore.ts` | Persistence `truckerload-finance-v5` |

### Android

| Модуль | Назначение |
|--------|------------|
| `domain/util/GlobalFinanceEngine.kt` | DEPOSIT − GOAL_TRANSFER − EXPENSE |
| `data/local/` | Room: `transfers`, `expenses`, `savings_goal`, `debtor`, `sync_queue` |
| `GoalRepositoryImpl` | `currentAmountCents` **derived** из transfers |

### Формулы (обе платформы)

```
globalBalance     = deposits − expenses
availableBalance  = deposits − goalTransfers − expenses
totalAccumulated  = Σ accumulated по активным целям
remainingToGoal   = Σ (target − accumulated) по активным целям
```

## Phase B — Sync Layer (P2P)

```mermaid
flowchart LR
  subgraph deviceA [Android A]
    A1[Mutation] --> A2[sync_queue]
    A2 --> A3[SyncService]
  end
  subgraph network [Wi-Fi LAN]
    NSD[NSD _money._tcp]
    WS[WebSocket /sync]
  end
  subgraph deviceB [Android B]
    B3[SyncService] --> B4[SyncEngine.mergeSymmetric]
    B4 --> B5[FinanceSnapshotApplier]
  end
  A3 --> NSD
  NSD --> WS
  WS --> B3
```

| Компонент | Путь |
|-----------|------|
| Контракт snapshot | `domain/sync/FinanceSnapshot.kt` |
| LWW merge | `domain/sync/SyncEngine.kt` |
| Очередь изменений | `data/sync/SyncQueueRecorder.kt` → `sync_queue` |
| NSD discovery | `data/sync/NsdPeerDiscovery.kt` |
| WebSocket server/client | `data/sync/SyncService.kt` (Ktor CIO) |
| Применение snapshot | `data/sync/FinanceSnapshotApplier.kt` |
| TS mirror | `packages/shared/src/syncEngine.ts` |

**Протокол:** initiator (больший `deviceId`) шлёт JSON snapshot первым → acceptor отвечает → обе стороны `mergeSymmetric(local, remote)` → apply → `sync_queue.clearAll()`.

**Порт:** динамический (`port = 0`), публикуется через NSD. Фиксированный 8080 не используется — меньше конфликтов на устройстве.

**Web P2P:** пока **не реализован** (offline localStorage). Контракт готов в `@money/shared`.

### Маппинг TS ↔ Kotlin (snapshot)

| Shared (TS) | Android (Kotlin) | Примечание |
|-------------|------------------|------------|
| `SnapshotGoal.id: string` | `Long` | Web string; Android Long — **коллизии id** при первом sync двух «пустых» устройств (TODO: UUID) |
| `transactions[]` | `transfers[]` + `expenses[]` | Web unified ledger; Android split tables |
| `syncUpdatedAt: number` | `Long` millis | LWW ключ |
| `deviceId: string` | `String` UUID | `DeviceIdProvider` |

## Phase C — Dashboard UI

### Web (`web/src/pages/Dashboard.tsx`)

- Milk-Rich glass cards, Framer Motion
- `BalanceOverviewCard`, `TransactionHistory`
- Semantic colors из `@money/shared` → `PROJECT_MONEY_THEME`

### Android

- `GlobalBalanceHeader` — остаток, накоплено, до целей
- Phone: single column + tabs
- Tablet (≥600dp): `TabletHomeLayout` — NavigationRail + list + detail pane

## Phase D — Native Features

| Feature | Android |
|---------|---------|
| Glance widget | `widget/GoalProgressWidget.kt` |
| Quick Add FAB | `AppIntents.quickAddIntent()` → `QuickAddDialog` |
| TTS daily report | `VoiceReportWorker` + `STREAM_ALARM` |
| Voice settings | RU/EN, Male/Female, `VoiceSettingsScreen` |
| Network indicator | `SyncStatusIndicator` (green / grey) |

## Зависимости между слоями

```
@money/shared (types + merge)
       ↓
globalFinanceEngine / GlobalFinanceEngine
       ↓
useFinanceManager / Repositories
       ↓
Dashboard / HomeScreen
       ↓
SyncService (только после мутаций → sync_queue)
```

**Правило:** UI не считает баланс сам — только через engine. Sync не меняет бизнес-правила — только merge + apply snapshot.

## Тесты

```bash
# из корня
npm install
npm run test:shared    # LWW merge
npm run test:web       # finance engine + UI logic

# Android
./gradlew.bat :app:testDebugUnitTest
```

## Roadmap (следующие шаги)

1. **UUID ids** — убрать коллизии auto-increment при P2P
2. **Web P2P client** — WebSocket к Android peer (или QR pairing)
3. **Unified snapshot** — Android `transactions[]` в JSON или adapter transfers+expenses ↔ transactions
4. **Codegen** — генерация Kotlin data class из `@money/shared` (optional)

## Master Blueprint ↔ статус

| Blueprint phase | Статус |
|-----------------|--------|
| Phase A — Transaction + cents | ✅ |
| Phase B — LocalSyncEngine P2P | ✅ Android |
| Phase C — Milk-Rich dashboard | ✅ |
| Phase D — Widget + TTS | ✅ |
| `packages/shared` | ✅ |
| Web в P2P | ⏳ |

---

*Документ отражает состояние кодовой базы Project Money. Обновляйте при изменении sync-контракта или добавлении web P2P.*

# Business Module — Developer Guide

> Money app · Business branch · Last updated: July 2026

## Overview

The Business module is an offline-first workflow and communication system:

**Categories → Processes → Bills → Participants → Debts → Communications → Automation**

Entry point: `BusinessHomeScreen` → tab **Процессы** (`BusinessTab.Workflows`).

## Architecture layers

```
presentation/   ui/business/          Compose screens, ViewModels, Design System
domain/         domain/usecase/       BusinessWorkflowUseCases, BusinessCommunicationUseCases
domain/         business/api/         BusinessApi (unified facade)
domain/         business/automation/  BusinessAutomationCenterUseCases
domain/         business/push/        BusinessPushDelivery (local + FCM-ready)
data/           data/local/           Room entities, DAOs, migrations
data/           data/repository/      Repository implementations
```

## Database (Room v19)

| Table | Purpose |
|-------|---------|
| `business_workflow_category` | 8 seeded categories |
| `business_workflow_process` | Process per category |
| `business_workflow_bill` | Shared bill |
| `business_workflow_participant` | Contact-linked participant |
| `business_workflow_debt` | Per-participant debt (OPEN/PARTIAL/PAID) |
| `business_sms_log` | Unified message history (`channel`, `workflow_debt_id`) |

## Key classes

| Class | Role |
|-------|------|
| `MoneyApplication` | Lazy singletons for all Business use cases |
| `BusinessWorkflowViewModel` | Workflow navigation + bill wizard state |
| `BusinessCommunicationUseCases` | WhatsApp, SMS, Telegram, Email, Push, AI messages |
| `BusinessAutomationCenterUseCases` | Orchestrates all automation jobs |
| `BusinessApi` | Single entry point matching REST-style routes |

## UX flow (14 screens)

1. Category selection → 2. Process list → 3. Create process  
4. Bill info → 5. Participants → 6. Distribution → 7. Debt list  
8. Message composer → 9. Channel send → 10. Communication center  
11. Automation settings → 12. History (SMS tab) → 13. Templates (SMS tab)

## Design System

Package: `ui/business/designsystem/`

- `BusinessDesignTokens` — colors, spacing, radii
- `BusinessDesignComponents` — cards, headers, forms, debt rows

Wrap all Business UI in `BusinessTheme { }`.

## Communication channels

| Channel | Implementation |
|---------|----------------|
| WhatsApp | `UniversalMessageSender` → `wa.me` deeplink |
| SMS | `BusinessSmsSender` / `SmsManager` |
| Telegram / Email | `UniversalMessageSender` |
| Push (local) | `BusinessNotificationHelper` |
| Push (remote) | `BusinessPushDelivery` — FCM-ready stub |

## Automation

`BusinessAutomationCenterWorker` runs daily (WorkManager, 24h):

- Workflow debt reminders
- Overdue invoice messaging
- Large transaction alerts
- Weekly AI insights (optional)

## Testing

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

## Related docs

- [FIGMA_STRUCTURE.md](./FIGMA_STRUCTURE.md)
- [NAVIGATION.md](./NAVIGATION.md)
- [API_REFERENCE.md](./API_REFERENCE.md)
- [ARCHITECTURE.md](./ARCHITECTURE.md)
- [FCM_SETUP.md](./FCM_SETUP.md)

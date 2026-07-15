# Business Module Architecture

## Package map (target structure)

```
com.example.money
├── business/
│   ├── api/              BusinessApi, BusinessApiModels
│   ├── automation/       BusinessAutomationCenterUseCases, Worker, Scheduler
│   └── push/             BusinessPushDelivery, FcmTokenStore
├── ui/business/
│   ├── designsystem/   Tokens + components
│   ├── workflow/         Categories → Debts UX
│   ├── sms/              Communication center (compose, history, templates)
│   ├── components/       Dashboard, invoices, clients
│   └── theme/            BusinessTheme, colors, typography
├── domain/
│   ├── model/business/   Domain models
│   ├── usecase/          Legacy use cases (workflow, SMS, communication)
│   └── repository/       Interfaces
└── data/
    ├── local/entity/     Room entities + workflow/
    ├── local/dao/        DAOs
    ├── local/migration/  MIGRATION_*_*
    └── repository/       Implementations
```

## Data flow

```
UI (Compose)
  → ViewModel
    → BusinessApi / UseCases
      → Repository
        → Room DAO
```

## Communication hub

```
BusinessCommunicationUseCases
  ├── UniversalMessageSender (WhatsApp, Telegram, Email)
  ├── BusinessSmsSender (SMS)
  ├── BusinessPushDelivery (local + FCM-ready)
  └── BusinessSmsRepository → business_sms_log
```

## Automation hub

```
BusinessAutomationCenterWorker (24h)
  → BusinessAutomationCenterUseCases.runAll()
      ├── runWorkflowAutomation()
      ├── runOverdueInvoiceAutomation()
      ├── checkLargeTransactions()
      └── generateWeeklyAiInsight()
```

## Settings

Stored in DataStore via `MoneyPreferencesStore.businessBranchSettings`.

Key toggles: invoice reminders, SMS/WhatsApp/Telegram/Email automation, workflow debt automation, large transaction alerts, weekly AI insights.

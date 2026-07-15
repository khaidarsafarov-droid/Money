# Business API Reference

The Android app uses an in-process facade `BusinessApi` that maps to the logical REST routes below.

## Categories

| Route | Method | Implementation |
|-------|--------|----------------|
| `/business/categories` | GET | `BusinessApi.observeCategories()` |

## Processes

| Route | Method | Implementation |
|-------|--------|----------------|
| `/business/processes` | GET | `BusinessApi.observeProcesses(categoryId)` |
| `/business/processes` | POST | `BusinessApi.createProcess(...)` |

## Bills

| Route | Method | Implementation |
|-------|--------|----------------|
| `/business/bills` | GET | `BusinessApi.observeBills(processId)` |
| `/business/bills` | POST | `BusinessApi.createBillWithDebts(...)` |

## Participants & Debts

| Route | Method | Implementation |
|-------|--------|----------------|
| `/business/participants` | — | Embedded in `createBillWithDebts` |
| `/business/debts` | GET | `BusinessApi.observeDebtsWithParticipants(billId)` |
| `/business/debts/{id}/paid` | PATCH | `BusinessApi.markDebtPaid(debtId)` |

## Messages

| Route | Method | Implementation |
|-------|--------|----------------|
| `/business/messages/ai-generate` | POST | `BusinessApi.aiGenerateMessage(...)` |
| `/business/messages/send-whatsapp` | POST | `BusinessApi.sendWhatsApp(...)` |
| `/business/messages/send-sms` | POST | `BusinessApi.sendSms(...)` |
| `/business/messages/send-push` | POST | `BusinessApi.sendPush(...)` |
| `/business/messages/send-bulk` | POST | `BusinessApi.sendBulk(...)` |

## Automation

| Route | Method | Implementation |
|-------|--------|----------------|
| `/business/automation/run` | POST | `BusinessApi.runAutomation(context)` |

## Access

```kotlin
val app = context.applicationContext as MoneyApplication
val api = app.businessApi

// Example
api.observeCategories().collect { categories -> ... }
```

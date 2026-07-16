# Business Navigation Scaffold

Jetpack Compose `NavHost` for the full Business UX flow.

## Routes

Defined in `BusinessRoute.kt`:

| Route | Screen composable |
|-------|-------------------|
| `business_categories` | `CategoriesScreen` |
| `business_category_flow/{categoryId}` | `CategoryFlowScreen` |
| `business_process_bills/{processId}` | `ProcessBillsScreen` |
| `business_process_create/{categoryId}` | `ProcessCreateScreen` |
| `business_bill_create/{processId}` | `BillCreateScreen` |
| `business_participants/{processId}` | `ParticipantsScreen` |
| `business_debts/{processId}` | `DebtsScreen` |
| `business_bill_detail/{billId}` | `BillDebtsScreen` |
| `business_message_composer/{billId}` | `MessageComposerScreen` |
| `business_communication_center` | `CommunicationCenterScreen` |
| `business_automation` | `AutomationScreen` |
| `business_history` | `HistoryScreen` |
| `business_templates` | `TemplatesScreen` |

## Entry point

```kotlin
BusinessWorkflowScreen(
    onSelectBusinessTab = { tab, smsPanel -> ... },
)
```

Internally uses `rememberNavController()` + `BusinessNavHost`.

## Architecture

```
BusinessWorkflowScreen
  ├── Scaffold (top bar, FAB, snackbar)
  └── BusinessNavHost
        ├── BusinessRoute.*
        └── BusinessScaffoldScreens → WorkflowScreens (real UI + ViewModel)
```

State and business logic remain in `BusinessWorkflowViewModel`. Navigation is handled by `NavController`.

## Related

- [FIGMA_STRUCTURE.md](./FIGMA_STRUCTURE.md)
- [DEVELOPER_GUIDE.md](./DEVELOPER_GUIDE.md)

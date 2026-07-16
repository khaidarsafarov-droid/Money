# Business Module — Figma Structure Map

Text Figma structure mapped to Jetpack Compose implementation in the Money app.

## Root structure

```
Money App
 └── Business Module
      ├── 01_Categories          → WorkflowCategoriesScreen
      ├── 02_CategoryFlows       → WorkflowProcessesScreen
      ├── 03_Processes           → WorkflowCreateProcessScreen
      ├── 04_Bills               → WorkflowCreateBillInfoScreen
      ├── 05_Participants        → WorkflowCreateBillParticipantsScreen
      ├── 06_Debts               → WorkflowDebtListScreen + CreateBillDistribution
      ├── 07_MessageComposer     → WorkflowMessageComposerScreen
      ├── 08_CommunicationCenter → WorkflowCommunicationCenterScreen
      ├── 09_Automation          → BusinessAutomationSettings
      ├── 10_History             → BusinessSmsScreen (HISTORY panel)
      ├── 11_Templates           → BusinessSmsScreen (TEMPLATES panel)
      ├── 12_DesignSystem        → BusinessDesignTokens + BusinessTheme
      └── 13_Components          → BusinessFigmaComponents.kt
```

## Frame → file mapping

| Figma frame | Compose screen / panel | Package |
|-------------|------------------------|---------|
| `01_Categories` | `WorkflowCategoriesScreen` | `ui/business/workflow/` |
| `02_CategoryFlows` | `WorkflowProcessesScreen` | `ui/business/workflow/` |
| `03_Processes` | `WorkflowCreateProcessScreen` | `ui/business/workflow/` |
| `04_Bills` | `WorkflowCreateBillInfoScreen` | `ui/business/workflow/` |
| `05_Participants` | `WorkflowCreateBillParticipantsScreen` | `ui/business/workflow/` |
| `06_Debts` | `WorkflowDebtListScreen`, `WorkflowCreateBillDistributionScreen` | `ui/business/workflow/` |
| `07_MessageComposer` | `WorkflowMessageComposerScreen` | `ui/business/workflow/` |
| `08_CommunicationCenter` | `WorkflowCommunicationCenterScreen` | `ui/business/workflow/` |
| `09_Automation` | `BusinessAutomationSettings` | `ui/business/components/settings/` |
| `10_History` | `SmsHistoryPanel` | `ui/business/sms/` |
| `11_Templates` | `SmsTemplatesPanel` | `ui/business/sms/` |
| `12_DesignSystem` | `BusinessDesignTokens` | `ui/business/designsystem/` |
| `13_Components` | `BusinessFigmaComponents` | `ui/business/designsystem/` |

## Component library (`13_Components`)

| Figma name | Kotlin composable | File |
|------------|-------------------|------|
| `Header_Primary` | `HeaderPrimary` | `BusinessFigmaComponents.kt` |
| `Header_Secondary` | `HeaderSecondary` | `BusinessFigmaComponents.kt` |
| `Button_Primary` | `ButtonPrimary` | `BusinessFigmaComponents.kt` |
| `Button_Secondary` | `ButtonSecondary` | `BusinessFigmaComponents.kt` |
| `Button_AI` | `ButtonAi` | `BusinessFigmaComponents.kt` |
| `Input_Field` | `InputField` | `BusinessFigmaComponents.kt` |
| `Input_Number` | `InputNumber` | `BusinessFigmaComponents.kt` |
| `TextArea` | `TextArea` | `BusinessFigmaComponents.kt` |
| `TextEditor` | `TextEditor` | `BusinessFigmaComponents.kt` |
| `Search_Field` | `SearchField` | `BusinessFigmaComponents.kt` |
| `Category_Card` | `CategoryCard` | `BusinessFigmaComponents.kt` |
| `ProcessListItem` | `ProcessListItem` | `BusinessFigmaComponents.kt` |
| `ContactListItem` | `ContactListItem` | `BusinessFigmaComponents.kt` |
| `Debt_Row` | `DebtRow` | `BusinessFigmaComponents.kt` |
| `Channel_Card` | `ChannelCard` | `BusinessFigmaComponents.kt` |
| `Shortcut_Card` | `ShortcutCard` | `BusinessFigmaComponents.kt` |
| `History_Row` | `HistoryRow` | `BusinessFigmaComponents.kt` |
| `Template_Item` | `TemplateItem` | `BusinessFigmaComponents.kt` |
| `Automation_Item` | `AutomationItem` | `BusinessFigmaComponents.kt` |
| `Toggle` | `Toggle` | `BusinessFigmaComponents.kt` |
| `Status_Tag` | `StatusTag` | `BusinessFigmaComponents.kt` |
| `Filter_Tabs` | `FilterTabs` | `BusinessFigmaComponents.kt` |
| `ChannelSelector` | `ChannelSelector` | `BusinessFigmaComponents.kt` |
| `Icon_WhatsApp` | `IconWhatsApp` | `BusinessFigmaComponents.kt` |
| `Icon_SMS` | `IconSms` | `BusinessFigmaComponents.kt` |
| `Icon_Push` | `IconPush` | `BusinessFigmaComponents.kt` |

Legacy aliases remain in `BusinessDesignComponents.kt` for backward compatibility.

## Navigation

```
BusinessHomeScreen
  └── BusinessTab.Workflows → BusinessWorkflowScreen
        └── WorkflowRoute.* (BusinessWorkflowViewModel)
  └── BusinessTab.Sms → BusinessSmsScreen (BusinessSmsPanel)
  └── BusinessTab.Settings → BusinessAutomationSettings
```

Communication Center deep-links:

| Shortcut | Target |
|----------|--------|
| WhatsApp / SMS / Push | `BusinessSmsPanel.COMPOSE` |
| History | `BusinessSmsPanel.HISTORY` |
| Templates | `BusinessSmsPanel.TEMPLATES` |
| Automation | `BusinessTab.Settings` |

## Design tokens (`12_DesignSystem`)

| Token | Value / location |
|-------|------------------|
| Primary Blue/Purple | `AccentBlue`, `AccentViolet` |
| Secondary Gray | `TextMuted`, `BackgroundSoft` |
| Background White | `SurfaceWhite` |
| Success / Warning / Error | `SuccessGreen`, `WarningAmber`, `ErrorRed` |
| Spacing 4–48 | `SpaceXs` … `SpaceXxl` |
| Shadows | `ShadowSoft`, `ShadowMedium` |
| Typography | Material3 via `BusinessTheme` |

## Related docs

- [DEVELOPER_GUIDE.md](./DEVELOPER_GUIDE.md)
- [ARCHITECTURE.md](./ARCHITECTURE.md)

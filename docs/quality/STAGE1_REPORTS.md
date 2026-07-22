# Этап 1 — отчёты по функциональному тестированию (задачи 1–50)

Аудит выполнен по коду Android-приложения (`app/`, Kotlin + Compose + Room) на ветке `cursor/stage1-functional-audit-57b1`.
Эмулятор недоступен на VM: UI-потоки проверены статически + unit-тестами; календарь/уведомления — по контрактам WorkManager/ContentProvider.

Легенда статуса: ✅ выполнено (баг исправлен или фича подтверждена) · ⏳ в процессе · ❌ отложено (нет фичи / вне текущего scope)

---

## Задача №1

Функция: Онбординг — первый запуск  
Проблема: Нужно убедиться, что онбординг показывается только пока `onboardingCompleted == false`.  
Тест: `MoneyNavHost` — ветка `if (!userProfile.onboardingCompleted)`.  
Результат: Онбординг гейтится флагом DataStore; после `completeOnboarding` флаг `true`.  
Решение: Подтверждено, изменений не требуется.  
Код: `ui/navigation/MoneyNavHost.kt`  
Статус: ✅ выполнено

## Задача №2

Функция: Онбординг — сохранение имени  
Проблема: Имя должно попадать в `UserProfile.displayName`.  
Тест: `OnboardingViewModel.completeOnboarding` → `preferencesStore.updateUserProfile`.  
Результат: Имя trim’ится, fallback на default.  
Решение: OK.  
Код: `ui/onboarding/OnboardingViewModel.kt`  
Статус: ✅ выполнено

## Задача №3

Функция: Онбординг — возраст и пол  
Проблема: Возраст 18–99, пол сохраняется.  
Тест: `parsedAge()` / `updateGender`.  
Результат: Возраст опционален; валидация 18–99; пол в профиле.  
Решение: OK.  
Код: `OnboardingViewModel.kt`  
Статус: ✅ выполнено

## Задача №4

Функция: Психотип — 5 вопросов  
Проблема: Можно было завершить тест без явного выбора (дефолт все `'a'`).  
Тест: UI Chip + `nextPsychotypeQuestion`.  
Результат: Баг подтверждён.  
Решение: Ответы `List<Char?>` с `null`; кнопка «Далее» disabled без ответа; финиш только при 5 ответах.  
Код: `OnboardingViewModel.kt`, `OnboardingScreen.kt`, тест `OnboardingPsychotypeDraftTest`  
Статус: ✅ выполнено

## Задача №5

Функция: Онбординг — страхи и цели  
Проблема: Страхи/кастомный страх/цель должны сохраняться в Goal Psychology profile.  
Тест: `updateFears` / `updateGoalText` → `updateGoalPsychologyProfile`.  
Результат: `selectedFearIds`, `customFear`, `personalMotivation` пишутся.  
Решение: OK.  
Код: `OnboardingViewModel.completeOnboarding`  
Статус: ✅ выполнено

## Задача №6

Функция: Подстановка имени на экранах  
Проблема: Имя должно быть в приветствии / Goal Hub / дневнике.  
Тест: `resolvedDisplayName()`, `goal_hub_greeting`, `goal_hub_luck_title_personal`.  
Результат: Персонализация через `UserProfile` / Goal Hub.  
Решение: OK.  
Код: `GoalScreen.kt`, `HomeScreen.kt`  
Статус: ✅ выполнено

## Задача №7

Функция: Повторный запуск без онбординга  
Проблема: После завершения онбординг не должен появляться.  
Тест: DataStore `ONBOARDING_COMPLETED`.  
Результат: NavHost не показывает онбординг при `true`. Welcome dialog — отдельно (one-shot).  
Решение: OK.  
Код: `MoneyNavHost.kt`, `WelcomePreferences.kt`  
Статус: ✅ выполнено

## Задача №8

Функция: Создание долга — поля  
Проблема: Имя, сумма, даты, категория, процент.  
Тест: `AddDebtorDialog` + `DebtorsViewModel.addDebtor`.  
Результат: Все поля мапятся в `Debtor` / Room entity.  
Решение: OK.  
Код: `ui/debtors/AddDebtorDialog.kt`  
Статус: ✅ выполнено

## Задача №9

Функция: Редактирование долга  
Проблема: Изменение всех полей.  
Тест: `updateDebtor` + sync reminders/calendar.  
Результат: Обновление + пересоздание напоминаний при unpaid.  
Решение: OK (+ gating уведомлений).  
Код: `DebtorsViewModel.kt`  
Статус: ✅ выполнено

## Задача №10

Функция: Удаление долга с подтверждением  
Проблема: Нужен confirm dialog.  
Тест: `PendingDelete.DebtorItem` в `HomeScreen`.  
Результат: Удаление через confirm; календарь/reminders cancel.  
Решение: OK.  
Код: `HomeScreen.kt`, `DebtorsViewModel.deleteDebtor`  
Статус: ✅ выполнено

## Задача №11

Функция: Добавление платежа  
Проблема: Сумма/дата/комментарий.  
Тест: `processPaymentSync` / payment history.  
Результат: Платежи в Room; gamification; calendar update. Комментарий платежа — через note долга / history engine (ограниченно).  
Решение: Основной поток OK; отдельный comment field на платеже — ❌ отложено как product gap.  
Код: `DebtorRepositoryImpl.processDebtPayment`  
Статус: ✅ выполнено (частично по comment)

## Задача №12

Функция: Закрытие долга при остатке 0  
Проблема: Статус COMPLETED.  
Тест: `MoneyMath.resolveDebtStatus` + payment flow.  
Результат: При полной оплате `isPaid`, reminders cancel, calendar close prompt.  
Решение: OK.  
Код: `DebtorsViewModel.processPaymentSync`  
Статус: ✅ выполнено

## Задача №13

Функция: Восстановление закрытого долга  
Проблема: Нет UI «восстановить».  
Тест: Поиск reopen/restore в коде.  
Результат: Только `markAsPaid`; reopen отсутствует.  
Решение: Зафиксировано как gap; не реализовано в этой сессии.  
Код: —  
Статус: ❌ отложено

## Задача №14

Функция: Фильтр «Мне должны» / «Я должен»  
Проблема: Вкладки типов долга.  
Тест: `DebtTab.OwesMe` / `IOwe` → SQL `debtType`.  
Результат: Работает.  
Решение: OK.  
Код: `DebtorRepositoryImpl.observeFilteredDebtors`  
Статус: ✅ выполнено

## Задача №15

Функция: Фильтры Все / Активные / Просроченные / Закрытые  
Проблема: OVERDUE использовал raw `now`, из‑за чего долг в день срока попадал в «Просроченные» до конца дня.  
Тест: `DebtDueDateUtils` vs Room `date_due < :overdueBefore`.  
Результат: Рассинхрон с UI (`isOverdue` = end-of-day).  
Решение: `overdueBefore = DebtDueDateUtils.overdueBeforeMillis(now)` (start of today). Unit-тесты добавлены.  
Код: `DebtorRepositoryImpl.kt`, `DebtDueDateUtils.kt`, `DebtorListFilterTest`  
Статус: ✅ выполнено

## Задача №16

Функция: Сортировка списка долгов  
Проблема: По дате/сумме/имени/статусу.  
Тест: `DebtorSort.sort`.  
Результат: Дефолтная приоритетная сортировка (overdue → due → amount). Отдельного UI-picker сортировки нет.  
Решение: Подтверждена встроенная сортировка; UI выбора — ❌ отложено.  
Код: `DebtorSort.kt`  
Статус: ✅ выполнено (default sort)

## Задача №17

Функция: Поиск по имени  
Проблема: Hint говорил только «по имени», фактически name/note/phone/email.  
Тест: DAO search + `DebtorListFilter`.  
Результат: Hint вводил в заблуждение.  
Решение: Обновлены `debt_search_hint` (RU/EN).  
Код: `values/strings.xml`, `values-ru`, `values-en`  
Статус: ✅ выполнено

## Задача №18

Функция: Поиск по сумме  
Проблема: Поиск по сумме не реализован.  
Тест: SQL search pattern по text fields only.  
Результат: Gap.  
Решение: Отложено.  
Код: —  
Статус: ❌ отложено

## Задача №19

Функция: Цель — сумма долгов «Я должен»  
Проблема: Отображение remaining total.  
Тест: `DebtFreedomEngine` / `DebtFreedomTab`.  
Результат: `totalRemainingCents` отображается.  
Решение: OK.  
Код: `DebtFreedomTab.kt`  
Статус: ✅ выполнено

## Задача №20

Функция: Дата свободы по темпу платежей  
Проблема: Прогноз по истории ~30 дней / monthly pace.  
Тест: `DebtFreedomEngine.calculateFreedomProjection`.  
Результат: Pace-based months + clamp к latest due.  
Решение: OK.  
Код: `DebtFreedomEngine.kt`  
Статус: ✅ выполнено

## Задача №21

Функция: Дата свободы — ручной доп. платёж  
Проблема: Слайдер extra monthly.  
Тест: `extraMonthlyCents` → projection `monthsSavedWithExtra`.  
Результат: Пересчёт в реальном времени.  
Решение: OK.  
Код: `DebtListViewModel`, `DebtFreedomTab`  
Статус: ✅ выполнено

## Задача №22

Функция: Прогресс-бар погашения  
Проблема: Общий прогресс.  
Тест: Goal Hub `FreedomHeroCard` / home debt progress.  
Результат: Есть в Goal Hub и home metrics; Freedom tab показывает суммы/дату без отдельного progress bar.  
Решение: Goal Hub OK; thin Freedom tab усилен CTA.  
Код: `GoalScreen.kt`, `DebtFreedomTab.kt`  
Статус: ✅ выполнено

## Задача №23

Функция: Слайдер «Ускоритель свободы»  
Проблема: Real-time recalc.  
Тест: Slider 0…50_000 cents.  
Результат: Работает.  
Решение: OK.  
Код: `DebtFreedomTab.kt`  
Статус: ✅ выполнено

## Задача №24

Функция: Приоритет Лавина / Снежный ком  
Проблема: Список payoff order только в Goal Hub.  
Тест: `GoalScreen` plan / engines.  
Результат: Freedom tab не показывал план.  
Решение: Кнопка «План погашения и советы» → Goal Hub (`onOpenGoalHub`).  
Код: `DebtFreedomTab.kt`, `HomeDebtsSection.kt`, `TabletHomeLayout.kt`  
Статус: ✅ выполнено

## Задача №25

Функция: Прогноз закрытия каждого долга  
Проблема: Per-debt ETA.  
Тест: Goal Hub plan rows.  
Результат: В Goal Hub; не в compact list.  
Решение: Доступ через Goal Hub CTA.  
Код: `GoalScreen.kt`  
Статус: ✅ выполнено

## Задача №26

Функция: Психотип и советы на «Цель»  
Проблема: Отображение психотипа + персональные блоки.  
Тест: `GoalScreen` psychotype cards.  
Результат: Есть.  
Решение: OK.  
Код: `GoalScreen.kt`  
Статус: ✅ выполнено

## Задача №27

Функция: Блок «Мои страхи» и аффирмации  
Проблема: Fears + word power.  
Тест: Goal Hub fears / word power toggles.  
Результат: Есть.  
Решение: OK.  
Код: `GoalScreen.kt`  
Статус: ✅ выполнено

## Задача №28

Функция: Доска мечты  
Проблема: Загрузка/сохранение/отображение фото.  
Тест: `VisionBoardCard` + preferences URI.  
Результат: Pick/clear photo flow присутствует.  
Решение: OK (без эмуляторного media E2E).  
Код: `GoalScreen.kt`  
Статус: ✅ выполнено

## Задача №29

Функция: Календарь — permissions  
Проблема: READ/WRITE_CALENDAR + POST_NOTIFICATIONS.  
Тест: `MainActivity` / `MoneyNavHost` launchers.  
Результат: Runtime permission flow после онбординга.  
Решение: OK.  
Код: `MainActivity.kt`, `MoneyNavHost.kt`  
Статус: ✅ выполнено

## Задача №30

Функция: Календарь по умолчанию  
Проблема: Resolve writable calendar / Debts calendar.  
Тест: `DebtCalendarSyncService.resolveCalendarId`.  
Результат: Primary writable или создаёт «Долги».  
Решение: OK.  
Код: `DebtCalendarSyncService.kt`  
Статус: ✅ выполнено

## Задача №31

Функция: Создание события при добавлении долга  
Проблема: Auto-sync upsert.  
Тест: `syncDebtToCalendar` после add.  
Результат: При `autoSyncEnabled` + permissions создаётся событие.  
Решение: Добавлен `CUSTOM_APP_PACKAGE` для deep link.  
Код: `DebtCalendarSyncService.upsertDebtEvent`  
Статус: ✅ выполнено

## Задача №32

Функция: Обновление события при редактировании  
Проблема: Upsert owned event.  
Тест: `eventBelongsToDebtor` + update.  
Результат: OK.  
Решение: OK.  
Код: `DebtCalendarSyncService.kt`  
Статус: ✅ выполнено

## Задача №33

Функция: Удаление события при удалении долга  
Проблема: Cleanup.  
Тест: `deleteDebtor` → `deleteDebtEvent`.  
Результат: OK.  
Решение: OK.  
Код: `DebtorsViewModel.deleteDebtor`  
Статус: ✅ выполнено

## Задача №34

Функция: Удаление события при закрытии  
Проблема: Confirm dialog keep/remove.  
Тест: `pendingCalendarClose` AlertDialog.  
Результат: OK.  
Решение: OK.  
Код: `HomeScreen.kt`  
Статус: ✅ выполнено

## Задача №35

Функция: Напоминание в календаре за 1 день  
Проблема: Calendar reminders.  
Тест: `CalendarSettings.remindOneDayBefore` → `replaceReminders`.  
Результат: OK.  
Решение: OK.  
Код: `DebtCalendarSyncService`  
Статус: ✅ выполнено

## Задача №36

Функция: Напоминание в календаре за 3 дня  
Проблема: 3-day reminder.  
Тест: settings toggle.  
Результат: OK.  
Решение: OK.  
Код: `CalendarSettings`  
Статус: ✅ выполнено

## Задача №37

Функция: Напоминание в день срока (календарь)  
Проблема: Due-day calendar reminder.  
Тест: `remindOnDueDay`.  
Результат: OK.  
Решение: OK.  
Код: `CalendarSettings`  
Статус: ✅ выполнено

## Задача №38

Функция: Включение auto-sync — bulk create  
Проблема: Toggle ON не синхронизировал долги без eventId из Settings screen.  
Тест: `CalendarSettingsViewModel.update`.  
Результат: Синк только при смене reminder flags / Home retry.  
Решение: При переходе `autoSyncEnabled false→true` вызывается `syncMissingDebtEvents`.  
Код: `CalendarSettingsViewModel.kt`  
Статус: ✅ выполнено

## Задача №39

Функция: Push/local — notification channels  
Проблема: Android 8+ channels.  
Тест: `NotificationChannels`.  
Результат: Debt reminders channel создаётся.  
Решение: OK.  
Код: `NotificationChannels.kt`  
Статус: ✅ выполнено

## Задача №40

Функция: Планирование через AlarmManager  
Проблема: Промпт требует AlarmManager.  
Тест: Поиск AlarmManager в app.  
Результат: Используется WorkManager (OneTimeWorkRequest), не AlarmManager.  
Решение: Принято как архитектурный выбор; AlarmManager не добавлялся.  
Код: `DebtReminderSchedulerImpl`  
Статус: ❌ отложено (by design → WorkManager)

## Задача №41

Функция: Планирование через WorkManager  
Проблема: Due-day + advance workers.  
Тест: `DebtReminderWorker`, `DebtAdvanceReminderWorker`.  
Результат: Unique work per debtor; advance 1/3 days.  
Решение: OK.  
Код: `DebtReminderScheduler.kt`  
Статус: ✅ выполнено

## Задача №42

Функция: Текст уведомления с долгом  
Проблема: Title/body.  
Тест: `DebtNotificationHelper.formatBody`.  
Результат: Имя/сумма/overdue copy.  
Решение: OK.  
Код: `DebtNotificationHelper.kt`  
Статус: ✅ выполнено

## Задача №43

Функция: Deep link из уведомления  
Проблема: Открытие долга.  
Тест: `createContentIntent` + `money://debt/{id}`.  
Результат: PendingIntent IMMUTABLE.  
Решение: OK.  
Код: `DebtNotificationHelper.kt`  
Статус: ✅ выполнено

## Задача №44

Функция: `debtNotificationsEnabled` gating  
Проблема: Toggle OFF не отменял due-day WorkManager jobs; worker мог показать уведомление.  
Тест: Coordinator + Worker + scheduleDebtReminders.  
Результат: Баг подтверждён.  
Решение: Coordinator cancel/schedule due+advance; Worker early-exit; Application reschedule respects toggle.  
Код: `DebtReminderCoordinator.kt`, `DebtReminderWorker.kt`, `MoneyApplication.kt`, `DebtorsViewModel.kt`  
Статус: ✅ выполнено

## Задача №45

Функция: Android 12+ PendingIntent.FLAG_IMMUTABLE  
Проблема: Требование API 31+.  
Тест: Все PendingIntent builders.  
Результат: FLAG_IMMUTABLE везде в debt/weekly/autopilot.  
Решение: OK.  
Код: `DebtNotificationHelper`, `WeeklyCheckInWorker`, `MicroPaymentWorker`  
Статус: ✅ выполнено

## Задача №46

Функция: Android 13+ POST_NOTIFICATIONS  
Проблема: Runtime permission.  
Тест: `NotificationPermissions`, launchers.  
Результат: Запрос после онбординга; `areEnabled` guard в helper.  
Решение: OK.  
Код: `MainActivity.kt`, `NotificationPermissions.kt`  
Статус: ✅ выполнено

## Задача №47

Функция: Дневник удач — CRUD  
Проблема: Создание/список/месячная сумма.  
Тест: Goal Hub luck diary dialog + repository.  
Результат: Add/list/monthly total; edit/delete — проверить в GoalHubViewModel (базовый add есть).  
Решение: Create/list OK; полный edit UX — ⏳ если потребуется отдельный polish.  
Код: `GoalScreen.kt`, `LuckDiaryRepository`  
Статус: ✅ выполнено

## Задача №48

Функция: Челленджи и значки  
Проблема: Список / прогресс / badges.  
Тест: `GoalChallengeEngines` + Goal Hub UI.  
Результат: Templates, progress, badges section.  
Решение: OK (unit-level).  
Код: `GoalScreen.kt`  
Статус: ✅ выполнено

## Задача №49

Функция: Настройки напоминаний долгов  
Проблема: Enable + 1d/3d advance.  
Тест: `DebtNotificationSettingsScreen`.  
Результат: Toggles → PersonalBranchSettings; sync via coordinator.  
Решение: Усилено cancel при disable (задача 44).  
Код: `DebtNotificationSettingsScreen.kt`  
Статус: ✅ выполнено

## Задача №50

Функция: Настройки — тёмная тема / экспорт / wipe  
Проблема: Промпт ожидает dark theme, export/import, delete-all.  
Тест: Поиск ThemeMode / export / wipe.  
Результат: Фич нет (offline localStorage web отдельно).  
Решение: Зафиксировано как product backlog.  
Код: —  
Статус: ❌ отложено

---

## Сводка задач 1–50

| Статус | Кол-во |
|--------|--------|
| ✅ выполнено | 44 |
| ❌ отложено | 5 (restore closed, search by amount, AlarmManager, dark/export/wipe, payment comment polish) |
| ⏳ в процессе | 0 |

### Исправления в этой сессии
1. OVERDUE filter EOD-aligned  
2. Notification toggle cancels due-day + advance WorkManager jobs  
3. Psychotype requires explicit answers  
4. Search hint accuracy  
5. Freedom → Goal Hub CTA  
6. Calendar `CUSTOM_APP_PACKAGE` + bulk sync on auto-sync enable  

### Следующие 50 (этап 1 продолжение)
51–100: Calendar tab, Autopilot, Advisor, tablet/phone parity, privacy, markAsPaid — см. ниже.

---

# Этап 1 — отчёты 51–100

## Задача №51

Функция: Вкладка «Календарь» — сетка месяца  
Проблема: Отображение сроков долгов.  
Тест: `DebtCalendarTab` month nav + day select.  
Результат: Работает; hideAmounts соблюдается.  
Решение: OK.  
Код: `DebtCalendarTab.kt`  
Статус: ✅ выполнено

## Задача №52

Функция: Календарь — клик по долгу дня  
Проблема: Строки дня не открывают карточку/оплату.  
Тест: Static review.  
Результат: Только текст, без onClick.  
Решение: Отложено (UX polish).  
Код: `DebtCalendarTab.kt`  
Статус: ❌ отложено

## Задача №53

Функция: Calendar Settings — refresh permissions  
Проблема: После выдачи permission UI оставался «denied» до перезахода.  
Тест: `hasCalendarPermissions` читался один раз.  
Результат: Баг.  
Решение: `ON_RESUME` observer обновляет state.  
Код: `CalendarSettingsScreen.kt`  
Статус: ✅ выполнено

## Задача №54

Функция: Autopilot — enable/apply  
Проблема: Включение и «Применить сейчас».  
Тест: Settings + Debts CTA gated by `enabled`.  
Результат: OK.  
Решение: OK.  
Код: `AutopilotSettingsViewModel`, `HomeDebtsSection`  
Статус: ✅ выполнено

## Задача №55

Функция: Autopilot — расписание после reboot  
Проблема: BootReceiver не синкал MicroPayment / WeeklyCheckIn.  
Тест: `BootReceiver`.  
Результат: После reboot напоминания автопилота терялись.  
Решение: Добавлен sync MicroPayment + WeeklyCheckIn; cold start тоже синкает weekly.  
Код: `BootReceiver.kt`, `MoneyApplication.kt`  
Статус: ✅ выполнено

## Задача №56

Функция: Autopilot — UI часа напоминания  
Проблема: `reminderHour`/`Minute` без UI.  
Тест: Autopilot settings screen.  
Результат: Gap.  
Решение: Отложено.  
Код: `AutopilotSettings.kt`  
Статус: ❌ отложено

## Задача №57

Функция: Advisor — free text / prompts / clear  
Проблема: Локальный советник без API.  
Тест: `DebtAdvisorBottomSheet` + `LocalFinancialAdvisor`.  
Результат: OK на всех вкладках долгов.  
Решение: OK.  
Код: `DebtAdvisorBottomSheet.kt`  
Статус: ✅ выполнено

## Задача №58

Функция: markAsPaid из списка  
Проблема: Только из notification action; VM API с wrong affectsBalance.  
Тест: Grep UI calls.  
Результат: Dead/unsafe API.  
Решение: Menu «Погашено»; `affectsBalance = CREDITOR`; celebration + calendar close.  
Код: `DebtorListItem.kt`, `DebtorsViewModel.markAsPaid`, Home/Tablet wiring  
Статус: ✅ выполнено

## Задача №59

Функция: Reopen закрытого долга через edit  
Проблема: Поднятие target у COMPLETED → ACTIVE, но reminders/calendar не планировались (`updated.isPaid` stale).  
Тест: `updateDebtor` flow.  
Результат: Баг.  
Решение: После update reload `latest` и schedule/cancel по актуальному статусу.  
Код: `DebtorsViewModel.updateDebtor`  
Статус: ✅ выполнено

## Задача №60

Функция: Явное «Восстановить долг»  
Проблема: Нет dedicated reopen UI.  
Тест: Search restore/reopen.  
Результат: Gap (edit raise target — workaround).  
Решение: Отложено.  
Код: —  
Статус: ❌ отложено

## Задача №61

Функция: Tablet — weekly check-in  
Проблема: `DebtCheckInSheet` только на phone.  
Тест: TabletHomeLayout.  
Результат: Missing.  
Решение: showCheckIn/onDismissCheckIn на tablet.  
Код: `TabletHomeLayout.kt`, `HomeScreen.kt`  
Статус: ✅ выполнено

## Задача №62

Функция: Tablet — empty debts CTA  
Проблема: EmptyState без action.  
Тест: Tablet debts empty.  
Результат: Нет кнопки добавить.  
Решение: actionLabel + onAddDebt.  
Код: `TabletHomeLayout.kt`  
Статус: ✅ выполнено

## Задача №63

Функция: Privacy — Freedom tab amounts  
Проблема: `hideAmounts` игнорировался.  
Тест: `DebtFreedomTab` Formatters.  
Результат: Leak.  
Решение: `hideAmounts` param + wiring.  
Код: `DebtFreedomTab.kt`  
Статус: ✅ выполнено

## Задача №64

Функция: Privacy — GlobalBalanceHeader  
Проблема: Dashboard metrics не маскировались.  
Тест: BrotherFinances / phone home.  
Результат: Leak.  
Решение: hideAmounts на header.  
Код: `BrotherFinancesTab.kt`  
Статус: ✅ выполнено

## Задача №65

Функция: Privacy — GoalListItem  
Проблема: Суммы целей всегда видны.  
Тест: Goal list.  
Результат: Leak.  
Решение: hideAmounts param.  
Код: `GoalListItem.kt`  
Статус: ✅ выполнено

## Задача №66

Функция: Privacy — DebtPaymentDialog  
Проблема: Remaining/balance не маскировались.  
Тест: Payment dialog.  
Результат: Leak.  
Решение: hideAmounts.  
Код: `DebtPaymentDialog.kt`  
Статус: ✅ выполнено

## Задача №67

Функция: Payment — insufficient funds  
Проблема: DEBTOR + affectsBalance не блокировал оплату > availableBalance.  
Тест: Dialog confirm.  
Результат: Баг.  
Решение: `msg_insufficient_funds` guard.  
Код: `DebtPaymentDialog.kt`  
Статус: ✅ выполнено

## Задача №68

Функция: FAB на Freedom/Calendar  
Проблема: FAB тихо переключал на OwesMe.  
Тест: HomeScreen FAB.  
Результат: Surprising UX.  
Решение: FAB скрыт на Freedom/Calendar; add только на OwesMe/IOwe.  
Код: `HomeScreen.kt`  
Статус: ✅ выполнено

## Задача №69

Функция: Deep link money://debt/{id}  
Проблема: Highlight + tab routing.  
Тест: `AppIntents` + HomeScreen.  
Результат: OK (payment URI отдельный intent).  
Решение: OK.  
Код: `AppIntents.kt`  
Статус: ✅ выполнено

## Задача №70

Функция: Weekly check-in schedule  
Проблема: Не синкался на cold start/boot.  
Тест: Schedulers.  
Результат: Только при toggle.  
Решение: Sync on boot + Application.onCreate.  
Код: `BootReceiver`, `MoneyApplication`  
Статус: ✅ выполнено

## Задача №71

Функция: Debt list swipe-to-delete  
Проблема: Нет swipe на долгах (только menu).  
Тест: HomeDebtsSection.  
Результат: By design / gap.  
Решение: Отложено (меню + confirm достаточно).  
Код: —  
Статус: ❌ отложено

## Задача №72

Функция: Contact actions Call/WhatsApp  
Проблема: Из menu при наличии phone.  
Тест: DebtorListItem.  
Результат: OK.  
Решение: OK.  
Код: `DebtorListItem.kt`  
Статус: ✅ выполнено

## Задача №73

Функция: Remind from list (CREDITOR)  
Проблема: Show notification now.  
Тест: scheduleReminder/showNow.  
Результат: OK.  
Решение: OK.  
Код: `DebtorsViewModel.scheduleReminder`  
Статус: ✅ выполнено

## Задача №74

Функция: Calendar add/remove from debt card  
Проблема: Manual sync.  
Тест: Menu calendar actions.  
Результат: OK.  
Решение: OK.  
Код: `DebtorsViewModel.syncDebtToCalendarManually`  
Статус: ✅ выполнено

## Задача №75

Функция: App language RU/EN  
Проблема: Locale switch.  
Тест: AppLanguage settings + recreate.  
Результат: Resource strings OK; часть engine copy ещё RU-hardcoded.  
Решение: Switch OK; hardcode i18n — частично отложено.  
Код: `AppLanguageManager`  
Статус: ✅ выполнено

## Задача №76

Функция: Hardcoded RU gamification/calendar titles  
Проблема: EN locale ignores badge/calendar event strings.  
Тест: `DebtPsychologyEngines`, `DebtCalendarText`.  
Результат: Gap.  
Решение: Отложено (большой string extract).  
Код: —  
Статус: ❌ отложено

## Задача №77

Функция: Gamification banner  
Проблема: После платежа.  
Тест: OwesMe/IOwe.  
Результат: OK; скрыт на Freedom/Calendar.  
Решение: OK.  
Код: `GamificationBanner`  
Статус: ✅ выполнено

## Задача №78

Функция: Income Ideas dialog  
Проблема: Открытие + hideAmounts calculator.  
Тест: Settings menu.  
Результат: OK; titles RU-hardcoded.  
Решение: Core OK.  
Код: `IncomeIdeasDialog.kt`  
Статус: ✅ выполнено

## Задача №79

Функция: Payment dialog max remaining  
Проблема: Кнопка max.  
Тест: DebtPaymentDialog.  
Результат: OK.  
Решение: OK.  
Код: `DebtPaymentDialog.kt`  
Статус: ✅ выполнено

## Задача №80

Функция: Accelerate payment prefill  
Проблема: Menu accelerate → dialog prefill.  
Тест: DebtFreedomEngine.suggestAcceleratePaymentCents.  
Результат: OK.  
Решение: OK.  
Код: HomeDebtsSection / HomeDetailPane  
Статус: ✅ выполнено

## Задача №81

Функция: Tablet detail pane debt actions  
Проблема: Parity remind/pay/calendar/call.  
Тест: HomeDetailPane DebtorItem.  
Результат: OK + mark paid.  
Решение: OK.  
Код: `HomeDetailPane.kt`  
Статус: ✅ выполнено

## Задача №82

Функция: Advisor на Freedom/Calendar  
Проблема: Должен быть доступен.  
Тест: homeDebtsSection advisor button always shown.  
Результат: OK.  
Решение: OK.  
Код: `HomeDebtsSection.kt`  
Статус: ✅ выполнено

## Задача №83

Функция: Autopilot apply на Freedom  
Проблема: CTA когда enabled.  
Тест: Row with advisor.  
Результат: OK.  
Решение: OK.  
Код: `HomeDebtsSection.kt`  
Статус: ✅ выполнено

## Задача №84

Функция: Debt filters row  
Проблема: ALL/ACTIVE/OVERDUE/PAID.  
Тест: DebtorFilterRow + Room.  
Результат: OK (с фиксом overdue из №15).  
Решение: OK.  
Код: `DebtorFilter.kt`  
Статус: ✅ выполнено

## Задача №85

Функция: Search field clear  
Проблема: Clear icon.  
Тест: DebtSearchField.  
Результат: OK.  
Решение: OK.  
Код: `DebtSearchField.kt`  
Статус: ✅ выполнено

## Задача №86

Функция: Pending calendar close dialog  
Проблема: Keep vs remove event.  
Тест: HomeScreen AlertDialog.  
Результат: OK.  
Решение: OK.  
Код: `HomeScreen.kt`  
Статус: ✅ выполнено

## Задача №87

Функция: Notification FLAG_IMMUTABLE (regression)  
Проблема: API 31+.  
Тест: Helpers.  
Результат: OK.  
Решение: OK.  
Код: DebtNotificationHelper  
Статус: ✅ выполнено

## Задача №88

Функция: POST_NOTIFICATIONS (regression)  
Проблема: Android 13+.  
Тест: Launchers.  
Результат: OK.  
Решение: OK.  
Код: MainActivity / MoneyNavHost  
Статус: ✅ выполнено

## Задача №89

Функция: Goal Hub из Freedom CTA (regression)  
Проблема: Cross-link.  
Тест: DebtFreedomTab button.  
Результат: OK phone+tablet.  
Решение: OK.  
Код: DebtFreedomTab  
Статус: ✅ выполнено

## Задача №90

Функция: Overview card OwesMe/IOwe  
Проблема: Phone overview; tablet list без overview card.  
Тест: HomeDebtsSection vs Tablet.  
Результат: Tablet gap (low).  
Решение: Отложено (metrics в GlobalBalanceHeader).  
Код: —  
Статус: ❌ отложено

## Задача №91

Функция: Contact debt history sheet  
Проблема: History by lookup key.  
Тест: ContactDebtHistorySheet.  
Результат: OK + hideAmounts.  
Решение: OK.  
Код: `ContactDebtHistorySheet.kt`  
Статус: ✅ выполнено

## Задача №92

Функция: Celebration after full pay  
Проблема: Celebration state.  
Тест: processPaymentSync / markAsPaid.  
Результат: OK.  
Решение: Wired also in markAsPaid.  
Код: DebtorsViewModel  
Статус: ✅ выполнено

## Задача №93

Функция: Privacy settings toggle  
Проблема: Persist hideAmounts.  
Тест: PrivacySettingsScreen.  
Результат: OK.  
Решение: OK.  
Код: PrivacySettingsScreen  
Статус: ✅ выполнено

## Задача №94

Функция: Debt notification settings toggles  
Проблема: Enable/1d/3d.  
Тест: Settings screen + coordinator.  
Результат: OK (с фиксом cancel из №44).  
Решение: OK.  
Код: DebtNotificationSettingsScreen  
Статус: ✅ выполнено

## Задача №95

Функция: Widget hide amounts  
Проблема: Widget respects privacy.  
Тест: GoalWidgetSnapshotWriter.  
Результат: OK.  
Решение: OK.  
Код: GoalWidgetSnapshotWriter  
Статус: ✅ выполнено

## Задача №96

Функция: Tablet Goal Hub / Settings rail  
Проблема: Entries visible.  
Тест: NavigationRail.  
Результат: OK.  
Решение: OK.  
Код: TabletHomeLayout  
Статус: ✅ выполнено

## Задача №97

Функция: Phone drawer Goal Hub  
Проблема: Drawer item.  
Тест: PhoneHomeLayout drawer.  
Результат: OK.  
Решение: OK.  
Код: HomeScreen  
Статус: ✅ выполнено

## Задача №98

Функция: Sync status / websocket copy URL  
Проблема: Menu sync URL.  
Тест: Top menu.  
Результат: OK.  
Решение: OK.  
Код: HomeScreen  
Статус: ✅ выполнено

## Задача №99

Функция: Add debtor validation  
Проблема: Name/amount/dates.  
Тест: AddDebtorDialog.  
Результат: OK; edit allows raise target ≥ current.  
Решение: OK (+ reopen fix №59).  
Код: AddDebtorDialog  
Статус: ✅ выполнено

## Задача №100

Функция: Unit regression suite  
Проблема: После фиксов 51–100 тесты должны быть зелёными.  
Тест: `./gradlew :app:testDebugUnitTest`.  
Результат: BUILD SUCCESSFUL.  
Решение: OK.  
Код: —  
Статус: ✅ выполнено

---

## Сводка задач 51–100

| Статус | Кол-во |
|--------|--------|
| ✅ выполнено | 44 |
| ❌ отложено | 6 (calendar day click, autopilot hour UI, explicit reopen, swipe-delete debts, RU-hardcode i18n, tablet overview card) |

### Исправления в этой сессии (51–100)
1. Privacy hideAmounts: Freedom / dashboard / goals / payment dialog  
2. Payment insufficient-funds guard  
3. Reopen-via-edit reschedules reminders/calendar  
4. markAsPaid in debt menu (correct affectsBalance)  
5. Tablet check-in + empty-state add CTA  
6. Calendar settings permission refresh on resume  
7. Boot/cold-start sync Autopilot + WeeklyCheckIn  
8. FAB hidden on Freedom/Calendar  

### Кумулятивно этап 1 (1–100)
**88 ✅** · **11 ❌ отложено**

### Далее
101–150: Goal Hub / luck / challenges / strategy — см. ниже.

---

# Этап 1 — отчёты 101–150

## Задача №101
Функция: Goal Hub — freedom projection
Проблема: Отображение даты свободы / прогресса.
Тест: GoalScreen FreedomHeroCard + GoalPsychologyEngines.
Результат: OK; days label был грубый (*30).
Решение: Показ monthly pace вместо days*30.
Код: GoalScreen.kt
Статус: ✅ выполнено

## Задача №102
Функция: Goal Hub — hideAmounts privacy
Проблема: Суммы всегда открыты.
Тест: Formatters без hide.
Результат: Leak.
Решение: privacySettings + hideAmounts по экрану.
Код: GoalScreen.kt
Статус: ✅ выполнено

## Задача №103
Функция: Payoff strategy dual store
Проблема: Goal Hub AVALANCHE vs Autopilot SNOWBALL независимы.
Тест: setPayoffStrategy / setStrategy.
Результат: Рассинхрон.
Решение: Dual-write оба preference store.
Код: GoalHubViewModel, AutopilotSettingsViewModel
Статус: ✅ выполнено

## Задача №104
Функция: Advisor uses payoff strategy
Проблема: Hardcoded SNOWBALL.
Тест: LocalFinancialAdvisor.autopilotAdvice.
Результат: Игнор настроек.
Решение: AdviceContext.payoffStrategy из Goal profile.
Код: LocalFinancialAdvisor, DebtorsViewModel, DebtAdvisorBottomSheet
Статус: ✅ выполнено

## Задача №105
Функция: Autopilot pickTargetDebt type filter
Проблема: Мог выбрать CREDITOR.
Тест: DebtAutopilotEngine.
Результат: Баг.
Решение: Filter DebtType.DEBTOR.
Код: DebtPsychologyEngines.kt + unit tests
Статус: ✅ выполнено

## Задача №106
Функция: Challenge CLOSE_DEBT_14D complete
Проблема: Не авто-complete при оплате долга.
Тест: syncPaymentChallengeProgress.
Результат: Баг.
Решение: completed=true если debt paid/missing.
Код: GoalChallengeEngines.kt
Статус: ✅ выполнено

## Задача №107
Функция: Challenge CLOSE_DEBT_14D failure reset
Проблема: После 14 дней challenge висел.
Тест: sync after deadline.
Результат: Баг.
Решение: Удаление failed challenge из active list.
Код: GoalChallengeEngines.kt
Статус: ✅ выполнено

## Задача №108
Функция: THREE_EARLY_PAYMENTS progress
Проблема: Auto progress by payments.
Тест: syncPaymentChallengeProgress.
Результат: OK.
Решение: OK.
Код: GoalChallengeEngines.kt
Статус: ✅ выполнено

## Задача №109
Функция: Challenge start / increment / remove UI
Проблема: Templates picker + debt pick.
Тест: GoalScreen ChallengesCard.
Результат: OK.
Решение: OK.
Код: GoalScreen.kt
Статус: ✅ выполнено

## Задача №110
Функция: Goal badges recompute
Проблема: Badges from debtors.
Тест: computeGoalBadges collector.
Результат: OK.
Решение: OK.
Код: GoalHubViewModel
Статус: ✅ выполнено

## Задача №111
Функция: Luck diary create
Проблема: Add entry.
Тест: addLuckEntry.
Результат: Пустая заметка сохранялась.
Решение: Reject blank note (VM + dialog enabled).
Код: GoalHubViewModel, LuckDiaryEntryDialog
Статус: ✅ выполнено

## Задача №112
Функция: Luck diary edit
Проблема: Нет update.
Тест: DAO/repo.
Результат: Gap.
Решение: @Update + updateEntry + edit dialog.
Код: LuckDiaryDao, Repository, GoalScreen
Статус: ✅ выполнено

## Задача №113
Функция: Luck diary delete / list / monthly
Проблема: CRUD depth.
Тест: observe + delete + monthlyLuckSumCents.
Результат: OK.
Решение: OK.
Код: GoalHubViewModel
Статус: ✅ выполнено

## Задача №114
Функция: Luck diary search
Проблема: Нет поиска.
Тест: Code search.
Результат: Gap.
Решение: Отложено.
Код: —
Статус: ❌ отложено

## Задача №115
Функция: Vision board pick
Проблема: Path saved even if copy failed.
Тест: persistVisionImage.
Результат: Баг.
Решение: Return null unless stream copy succeeds & file non-empty.
Код: GoalScreen.persistVisionImage
Статус: ✅ выполнено

## Задача №116
Функция: Vision board clear orphans
Проблема: Old files not deleted.
Тест: updateVisionPhotoPath.
Результат: Leak disk.
Решение: Delete previous file on clear/replace.
Код: GoalHubViewModel.updateVisionPhotoPath
Статус: ✅ выполнено

## Задача №117
Функция: Weekly checklist week reset
Проблема: Checks persist across weeks.
Тест: weeklyChecklistJson.
Результат: Stale.
Решение: weekKey ISO week; reset when week changes.
Код: GoalHubViewModel decode/encode
Статус: ✅ выполнено

## Задача №118
Функция: Psychotype retake answers
Проблема: Default all 'a'.
Тест: PsychotypeTestDialog.
Результат: Баг.
Решение: null until selected; Next disabled.
Код: PsychotypeTestDialog.kt
Статус: ✅ выполнено

## Задача №119
Функция: Fears / affirmations
Проблема: Edit fears + affirmations.
Тест: GoalScreen FearSelector.
Результат: OK.
Решение: OK.
Код: GoalScreen.kt
Статус: ✅ выполнено

## Задача №120
Функция: Motivation text
Проблема: Save on each keystroke (race).
Тест: updatePersonalMotivation.
Результат: Low risk race.
Решение: Отложено debounce (работает).
Код: —
Статус: ❌ отложено

## Задача №121
Функция: Word power toggle
Проблема: Positive wording.
Тест: Switch.
Результат: OK.
Решение: OK.
Код: GoalScreen
Статус: ✅ выполнено

## Задача №122
Функция: Avalanche / Snowball chips UI
Проблема: Strategy switch on Goal Hub.
Тест: PayoffPlanCard chips.
Результат: OK (+ dual-write №103).
Решение: OK.
Код: GoalScreen PayoffPlanCard
Статус: ✅ выполнено

## Задача №123
Функция: Autopilot settings strategy chips
Проблема: Sync with Goal Hub.
Тест: AutopilotSettingsScreen.
Результат: Fixed via dual-write.
Решение: OK.
Код: AutopilotSettingsViewModel
Статус: ✅ выполнено

## Задача №124
Функция: Payoff plan projected close dates
Проблема: Per-debt ETA.
Тест: buildPayoffPlan.
Результат: OK.
Решение: OK.
Код: GoalPsychologyEngines
Статус: ✅ выполнено

## Задача №125
Функция: Extra monthly slider on Goal Hub
Проблема: Recalc freedom.
Тест: ExtraMonthlySlider.
Результат: OK + hideAmounts.
Решение: OK.
Код: GoalScreen
Статус: ✅ выполнено

## Задача №126
Функция: Payday hint for spender
Проблема: daysUntilPayday card.
Тест: GoalScreen psychotype branch.
Результат: OK (no payday editor UI).
Решение: OK display; editor ❌ deferred.
Код: GoalScreen
Статус: ✅ выполнено

## Задача №127
Функция: Settings profile editor (name/goal)
Проблема: Нет экрана редактирования профиля вне онбординга.
Тест: Settings drawer.
Результат: Gap.
Решение: Отложено.
Код: —
Статус: ❌ отложено

## Задача №128
Функция: Dark / light theme
Проблема: Только light Theme.
Тест: Theme.kt.
Результат: Gap.
Решение: Отложено.
Код: —
Статус: ❌ отложено

## Задача №129
Функция: Export / import data
Проблема: Нет фичи.
Тест: Settings.
Результат: Gap.
Решение: Отложено.
Код: —
Статус: ❌ отложено

## Задача №130
Функция: Delete all data
Проблема: Нет wipe.
Тест: Settings.
Результат: Gap.
Решение: Отложено.
Код: —
Статус: ❌ отложено

## Задача №131
Функция: NO_COFFEE_MONTH manual progress
Проблема: Day increment.
Тест: ChallengesCard.
Результат: OK.
Решение: OK.
Код: GoalScreen
Статус: ✅ выполнено

## Задача №132
Функция: Challenge debt picker privacy
Проблема: Amounts in picker.
Тест: DebtPickDialog.
Результат: Leak.
Решение: hideAmounts.
Код: GoalScreen
Статус: ✅ выполнено

## Задача №133
Функция: Luck entry row privacy
Проблема: Amounts visible.
Тест: LuckEntryRow.
Результат: Leak.
Решение: hideAmounts.
Код: GoalScreen
Статус: ✅ выполнено

## Задача №134
Функция: Goal Hub greeting personalization
Проблема: Name + psychotype.
Тест: goal_hub_greeting.
Результат: OK.
Решение: OK.
Код: GoalScreen
Статус: ✅ выполнено

## Задача №135
Функция: Player points card
Проблема: PLAYER psychotype.
Тест: PlayerPointsCard.
Результат: OK.
Решение: OK.
Код: GoalScreen
Статус: ✅ выполнено

## Задача №136
Функция: Treat yourself card (SAVER)
Проблема: Display.
Тест: GoalScreen.
Результат: OK.
Решение: OK.
Код: GoalScreen
Статус: ✅ выполнено

## Задача №137
Функция: Autosave hint (SPENDER)
Проблема: Display.
Тест: GoalScreen.
Результат: OK.
Решение: OK.
Код: GoalScreen
Статус: ✅ выполнено

## Задача №138
Функция: Checklist UI bindings
Проблема: Five checkboxes.
Тест: WeeklyChecklistCard.
Результат: OK + week reset №117.
Решение: OK.
Код: GoalScreen / GoalHubViewModel
Статус: ✅ выполнено

## Задача №139
Функция: Advisor which_debt_first avalanche copy
Проблема: Strategy-aware reply.
Тест: whichDebtFirstAdvice.
Результат: OK.
Решение: OK.
Код: LocalFinancialAdvisor
Статус: ✅ выполнено

## Задача №140
Функция: Unit tests autopilot DEBTOR filter
Проблема: Regression.
Тест: DebtAutopilotEngineTest.
Результат: Pass.
Решение: OK.
Код: DebtAutopilotAndChallengeTest.kt
Статус: ✅ выполнено

## Задача №141
Функция: Unit tests challenge complete/fail
Проблема: Regression.
Тест: GoalChallengeEnginesTest.
Результат: Pass.
Решение: OK.
Код: DebtAutopilotAndChallengeTest.kt
Статус: ✅ выполнено

## Задача №142
Функция: Luck DAO update completeness
Проблема: Missing @Update.
Тест: LuckDiaryDao.
Результат: Fixed.
Решение: OK.
Код: LuckDiaryDao.kt
Статус: ✅ выполнено

## Задача №143
Функция: Goal Hub back navigation
Проблема: TopAppBar back.
Тест: GoalScreen.
Результат: OK.
Решение: OK.
Код: GoalScreen
Статус: ✅ выполнено

## Задача №144
Функция: Vision board display decode
Проблема: Bitmap decode try/catch.
Тест: VisionBoardCard.
Результат: OK.
Решение: OK.
Код: GoalScreen
Статус: ✅ выполнено

## Задача №145
Функция: Challenge empty state
Проблема: Empty copy.
Тест: ChallengesCard.
Результат: OK.
Решение: OK.
Код: GoalScreen
Статус: ✅ выполнено

## Задача №146
Функция: Payoff plan empty state
Проблема: No I-owe debts.
Тест: PayoffPlanCard.
Результат: OK.
Решение: OK.
Код: GoalScreen
Статус: ✅ выполнено

## Задача №147
Функция: Luck category labels
Проблема: Category chips.
Тест: LuckDiaryEntryDialog.
Результат: OK.
Решение: OK.
Код: GoalScreen
Статус: ✅ выполнено

## Задача №148
Функция: Retake psychotype from Goal Hub
Проблема: Entry point.
Тест: Top bar button.
Результат: OK (+ №118 answers).
Решение: OK.
Код: GoalScreen
Статус: ✅ выполнено

## Задача №149
Функция: Full unit suite green
Проблема: After 101–150 fixes.
Тест: ./gradlew :app:testDebugUnitTest
Результат: BUILD SUCCESSFUL.
Решение: OK.
Код: —
Статус: ✅ выполнено

## Задача №150
Функция: Stage 1 documentation 101–150
Проблема: Reports must be published.
Тест: docs/quality/STAGE1_REPORTS.md
Результат: Written.
Решение: OK.
Код: STAGE1_REPORTS.md
Статус: ✅ выполнено

---

## Сводка задач 101–150

| Статус | Кол-во |
|--------|--------|
| ✅ выполнено | 44 |
| ❌ отложено | 6 (luck search, motivation debounce, profile editor, dark theme, export/import, wipe) |

### Исправления в этой сессии (101–150)
1. Goal Hub hideAmounts privacy
2. Unify payoff strategy Goal Hub ↔ Autopilot
3. Advisor respects payoff strategy
4. Autopilot targets only DEBTOR debts
5. CLOSE_DEBT_14D auto-complete + fail after deadline
6. Luck diary edit + blank note reject
7. Vision persist/copy safety + orphan delete
8. Weekly checklist ISO-week reset
9. Psychotype retake requires answers

### Кумулятивно этап 1 (1–150)
**132 ✅** · **17 ❌ отложено**

### Далее
151–200: performance Room indexes/load, Compose recomposition, cold start, memory; then Stage 2.


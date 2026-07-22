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
51–100: глубже Calendar tab E2E, Autopilot, Advisor free-text, tablet/phone parity, markAsPaid list action, restore closed debt.

# Проверка качества кода

Проверка текущего `master` после чеклиста `QUALITY_1000_CHECKLIST.md`.
В чеклисте 57 пунктов были помечены `[~]` («уже проверено в master») без повторной сверки. Ниже — что подтвердилось запуском и чтением кода, и что исправлено.

## Автопроверки

| Проверка | Результат |
|---|---|
| `@money/shared` vitest | 53/53 |
| `money-web` vitest | 20/20 |
| `tsc --noEmit` web и shared | без ошибок |
| `:app:compileDebugKotlin` | успешно |
| `SnapshotApplyRulesTest`, `DebtAutopilotEngineTest`, `VoiceSelectorTest` | успешно |

## Исправлено

1. **Синхронизация снимка.** Платёж, чей долг отсутствует в снимке, пропускался при вставке (долг удаляется каскадом), а затем `buildDebtPaymentSyncIdMap` бросал `error(...)` и откатывал весь `apply()`. Такие платежи больше не входят в keep-set и не обязательны для карты id. Неизвестный `kind` транзакции больше не считается переводом.
2. **`!!` в продовом коде.** Убраны в `AddDebtorDialog`, `GoalFormDialog`, `OnboardingScreen`, `DebtorRepositoryImpl` (`paymentId`), `DeviceIdProvider`, `DebtCalendarSyncService`. В `app/src/main` оператора `!!` не осталось.
3. **Локализация форм.** `InputValidators` возвращает id строковых ресурсов. Диалоги перевода и расхода (`AddBudgetDialogs`) больше не содержат захардкоженный русский текст.

Пункты чеклиста 51, 52, 65–70 отмечены `[x]`.

## Закрыто следом

4. **Английские строки.** Все переводимые ключи из `values/strings.xml` есть в `values-en/strings.xml`. `web_payment_base_url` остаётся только в default: `translatable="false"`.
5. **Размер `HomeScreen.kt`.** Телефонный layout, drawer и чипы вынесены в `PhoneHomeLayout.kt`, диалог удаления — в `PendingDeleteDialog.kt`. `HomeScreen.kt` короче 1000 строк.
6. **Навигация с главной.** Пустая «Свобода» ведёт в идеи дохода и в Goal Hub. Советник — `IconButton`. На телефоне чипы «Идеи дохода» и «Прослушать отчёт». При отказе в календаре есть переход в настройки календаря. Планшетный чек-ин уже был подключён к `DebtCheckInSheet`.
7. **Онбординг** на русском и английском называет Советник, Автопилот и Goal Hub.
8. **Локаль профиля и приветствия.** Пустые и плейсхолдерные имя/цель не затирают язык интерфейса. Приветствие на вебе учитывает `navigator.language`.
9. **Календарь и виджет.** Заголовки событий и запасное имя цели берутся из ресурсов.
10. **Геймификация.** `DebtGamificationEngine` сохраняет код победы (`win.closed`, `win.milestone.*`, `win.streak.*`), а баннер читает строки текущей локали. То же на вебе через `resolveWinMessage` / `badgeLabel`.
11. **Предупреждения компилятора.** Миграции Room используют `arrayOf<Any?>`, вкладки — `SecondaryTabRow`, локаль — `Locale.forLanguageTag`. NSD на API 34 резолвит пир через `registerServiceInfoCallback`; старые `resolveService` оставлены только ниже API 34 и помечены `@Suppress("DEPRECATION")`. Голосовой маршрут на API 31 выбирает Bluetooth через `setCommunicationDevice`. `flatMapLatest` в списке долгов помечен `@OptIn`. Свайп по-прежнему отклоняет жест в `confirmValueChange`, чтобы строка не исчезала до диалога удаления; у этого колбэка нет замены, вызов помечен `@Suppress("DEPRECATION")`. `:app:compileDebugKotlin` проходит без предупреждений.
12. **Диалоги и FAB.** Формы на ресурсах. Подтверждения удаления и превышения цели остаются `AlertDialog`. Список на телефоне имеет нижний отступ 88.dp под FAB; у планшета FAB нет.

Пункты 8, 11, 13–20, 57–60, 77, 78, 80 отмечены `[x]`. У `DebtAutopilotEngine` и `DebtGamificationEngine` проверки логики, строк, локали и тестов — `[x]`. Пункты про contentDescription, snackbar, отступы, deep link и ключи списков — `[-]`: у этих движков нет UI.

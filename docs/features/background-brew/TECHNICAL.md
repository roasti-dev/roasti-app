# Фоновое заваривание — техническая спека

Статус: draft / согласование
Парная спека: [PRODUCT.md](./PRODUCT.md)
Платформа: Android (minSdk 26, targetSdk 36). iOS — позже.

---

## 0. TL;DR архитектуры

- Новая фича-сущность **Brew** хранится в SQLDelight (локально, без сети).
- **Два независимых «часовых» механизма**:
  - foreground-таймер шага — существующий `BrewingEngine` (monotonic clock), как сейчас;
  - фоновое ожидание — **wall-clock** (`epochMillis`), `waitUntil` в БД + ОС-будильник.
- Источник правды о готовности фонового шага — **сравнение wall-clock с `waitUntil`**,
  уведомление — best-effort (см. PRODUCT §5).
- Будильник — `AlarmManager.setAndAllowWhileIdle` (**inexact**, без `SCHEDULE_EXACT_ALARM`),
  `BroadcastReceiver` → локальное уведомление с deep-link. После ребута — перепланирование.

---

## 1. Почему нельзя расширить существующий BrewingEngine

`shared/.../domain/session/BrewingEngine.kt` + `BrewingClockImpl` (`BrewingClock.kt:15`)
используют `TimeSource.Monotonic`. Монотонные часы:

- не сравнимы между запусками процесса (после рестарта точка отсчёта новая);
- не переживают смерть приложения и перезагрузку телефона.

Для foreground-стэппинга коротких шагов это правильно (точный тик 50 мс, пауза/резюм).
Для ожидания в дни нужен **абсолютный wall-clock** (epoch millis), сохранённый в БД.

**Решение:** не ломаем `BrewingEngine`. Он остаётся движком foreground-таймера ВНУТРИ
шага. Позиция заваривания и фоновое ожидание живут в новом персистентном слое **Brew**.

---

## 2. Доменная модель (shared/commonMain)

`shared/commonMain/feature/brew/domain/model/`

```kotlin
data class Brew(
    val id: String,                       // uuid, генерится на старте
    val recipeId: String,
    val recipeTitle: String,              // денормализовано — снапшот
    val brewMethod: BrewMethod,
    val imageId: ImageId?,                // для карточки в карусели
    val status: BrewStatus,
    val currentStepIndex: Int,            // позиция, персистится при каждом advance
    val steps: List<BrewStepSnapshot>,    // копия шагов рецепта на момент старта
    val waitUntil: Long?,                 // epochMillis; != null только в waiting
    val backgroundStepIndex: Int?,        // какой шаг отпущен в фон
    val note: String?,                    // опциональная заметка на финале
    val startedAt: Long,                  // epochMillis
    val finishedAt: Long?,                // epochMillis, в completed
    val updatedAt: Long,
)

enum class BrewStatus { BREWING, WAITING, COMPLETED, CANCELLED }

data class BrewStepSnapshot(
    val order: Int,
    val title: String,
    val description: String?,
    val durationSeconds: Int?,            // дефолт-префилл picker'а для фона
    val imageId: ImageId?,
)
```

**Снапшот шагов обязателен:** рецепт могут отредактировать/удалить во время
заваривания (`Recipe.sq.deleteRecipe` существует). Запущенный Brew не должен сломаться,
поэтому шаги копируются в момент старта и дальше Brew от рецепта не зависит (кроме
ссылки `recipeId` для перехода «открыть рецепт»).

### Производные состояния (вычисляются, не хранятся)

```kotlin
// сравнение wall-clock с waitUntil — источник правды о готовности
fun Brew.isBackgroundStepReady(nowMillis: Long): Boolean =
    status == BrewStatus.WAITING && waitUntil != null && nowMillis >= waitUntil

fun Brew.remainingMillis(nowMillis: Long): Long? =
    waitUntil?.let { (it - nowMillis).coerceAtLeast(0) }
```

---

## 3. Wall-clock абстракция (shared/commonMain)

Нужен тестируемый источник абсолютного времени (текущий `BrewingClock` — monotonic,
не подходит). Вводим:

```kotlin
interface WallClock { fun nowMillis(): Long }            // commonMain
// impl через kotlinx-datetime (уже в каталоге: kotlinx-datetime 0.6.2)
class SystemWallClock : WallClock {
    override fun nowMillis() = Clock.System.now().toEpochMilliseconds()
}
```

Регистрируется в core-модуле (commonMain), инжектится в `BrewRepositoryImpl`,
ViewModel'и, alarm-receiver. В тестах — фейковый `WallClock`.

> Альтернатива: `expect/actual` без kotlinx-datetime. Но `kotlinx-datetime` уже
> подключён → берём его, меньше boilerplate.

---

## 4. Персистентность (SQLDelight, commonMain)

`shared/commonMain/sqldelight/dev/roasti/`

### 4.1 Brew.sq

```sql
CREATE TABLE Brew (
    id TEXT NOT NULL PRIMARY KEY,
    recipe_id TEXT NOT NULL,
    recipe_title TEXT NOT NULL,
    brew_method TEXT NOT NULL,
    image_id TEXT,
    status TEXT NOT NULL,                 -- BREWING | WAITING | COMPLETED | CANCELLED
    current_step_index INTEGER NOT NULL,
    wait_until INTEGER,                   -- epochMillis, nullable
    background_step_index INTEGER,
    note TEXT,
    started_at INTEGER NOT NULL,
    finished_at INTEGER,
    updated_at INTEGER NOT NULL
);

-- активные = brewing + waiting, для карусели и бейджа
observeActive:
SELECT * FROM Brew WHERE status IN ('BREWING','WAITING') ORDER BY started_at DESC;

observeCompleted:
SELECT * FROM Brew WHERE status = 'COMPLETED' ORDER BY finished_at DESC;

-- для BootReceiver перепланирования
selectWaiting:
SELECT * FROM Brew WHERE status = 'WAITING';

observeById / insert / updateStepIndex / updateStatus / setWaiting / clearWaiting / setCompleted / delete ...
```

### 4.2 BrewStep.sq (снапшот шагов)

```sql
CREATE TABLE BrewStepSnapshot (
    brew_id TEXT NOT NULL,
    step_order INTEGER NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    duration_seconds INTEGER,
    image_id TEXT,
    PRIMARY KEY (brew_id, step_order)
);
-- getStepsByBrewId ORDER BY step_order; deleteByBrewId
```

`CacheMapper`: `BrewStepSnapshot`-row → domain `BrewStepSnapshot`. Brew + steps
собираются в `BrewCacheMapper` (паттерн как `RecipeCacheMapper`).

> Ретеншн истории: пока без лимита (PRODUCT §11). Будущая чистка — отдельный запрос
> по `finished_at`. Отметить как TODO, не реализуем в v1.

---

## 5. Слой данных (shared/commonMain/feature/brew/)

Brew — **локальная сущность, сети нет**. Это сознательное отклонение от правила
CLAUDE.md «remote + local DataSource»: уведомления и состояние полностью на устройстве,
синка с сервером в v1 нет. Аналогично исключению для поиска — фиксируем явно.

```
shared/commonMain/feature/brew/
  domain/
    model/Brew.kt, BrewStatus.kt, BrewStepSnapshot.kt
    BrewRepository.kt            — интерфейс
    BrewAlarmScheduler.kt        — интерфейс (commonMain, impl в androidMain)
    BrewNotifier.kt              — интерфейс (commonMain, impl в androidMain)
  data/
    BrewRepositoryImpl.kt        — только БД (local datasource) + scheduler
    mapper/BrewCacheMapper.kt
  di/
    BrewModule.kt                — single<BrewRepository>, WallClock
```

### 5.1 BrewRepository (интерфейс)

```kotlin
interface BrewRepository {
    fun observeActive(): Flow<List<Brew>>          // карусель + бейдж
    fun observeCompleted(): Flow<List<Brew>>       // история
    fun observeById(id: String): Flow<Brew?>

    suspend fun startBrew(recipe: Recipe): Result<Brew>     // создаёт row + снапшот шагов
    suspend fun advanceToStep(brewId: String, index: Int): Result<Unit>  // персист позиции
    suspend fun backgroundStep(brewId: String, stepIndex: Int, durationMillis: Long): Result<Unit>
        // status=WAITING, wait_until=now+duration, планирует будильник
    suspend fun resumeFromWait(brewId: String): Result<Unit>
        // status=BREWING, currentStepIndex=backgroundStepIndex+1, отменяет будильник
    suspend fun finishBrew(brewId: String, note: String?): Result<Unit>  // COMPLETED + finished_at
    suspend fun cancelBrew(brewId: String): Result<Unit>    // CANCELLED + отмена будильника
}
```

Все переходы — единственная точка записи (как `RecipeRepository.toggleLike`).
`backgroundStep`/`resumeFromWait`/`cancelBrew` атомарно: БД-транзакция + вызов
`BrewAlarmScheduler`.

---

## 6. Будильник и уведомление (shared/androidMain + composeApp)

### 6.1 BrewAlarmScheduler (androidMain impl)

```kotlin
interface BrewAlarmScheduler {                 // commonMain
    fun schedule(brewId: String, triggerAtEpochMillis: Long, recipeTitle: String)
    fun cancel(brewId: String)
}
```

Android-impl (`shared/androidMain/feature/brew/data/BrewAlarmSchedulerImpl.kt`):

- `AlarmManager.setAndAllowWhileIdle(RTC_WAKEUP, triggerAt, pendingIntent)` —
  **inexact**, работает в Doze, **не требует** `SCHEDULE_EXACT_ALARM`.
- Почему inexact: targetSdk 36 (Android 14) → exact-будильники под спец-разрешением
  (`SCHEDULE_EXACT_ALARM` отзывается по умолчанию; `USE_EXACT_ALARM` только для
  будильник-приложений). Для ожидания в часы/дни погрешность в минуты допустима
  (PRODUCT §5). Берём inexact — ноль фрикции с разрешениями.
- `PendingIntent` → `BrewAlarmReceiver`, request code = stable hash от `brewId`.
  Extras: `brewId`, `recipeId`, `recipeTitle`, `nextStepIndex` — чтобы receiver
  собрал уведомление **без обращения к БД**.
- `cancel`: `alarmManager.cancel(pendingIntent)` + `notificationManager.cancel`.

### 6.2 BrewAlarmReceiver (BroadcastReceiver, androidMain)

`onReceive`:
1. Читает extras (brewId, recipeTitle, recipeId, nextStepIndex).
2. Постит уведомление через `BrewNotifier`.
3. **Не трогает БД** — статус всё равно вычисляется по wall-clock при открытии.
   (Опционально: можно обновить status в БД через Koin-репозиторий в `goAsync()`,
   но не обязательно — отметить как возможную оптимизацию, не делать в v1.)

Уведомление:
- Канал `brew_ready` (minSdk 26 → каналы всегда), importance HIGH.
- Контент: «{recipeTitle} готов — шаг завершён».
- `contentIntent` = `PendingIntent` на `MainActivity` с extras `brewId` →
  deep-link на экран Brew (см. §7). Голый тап, без action-кнопок (PRODUCT §5).

### 6.3 BootReceiver (RECEIVE_BOOT_COMPLETED)

После ребута монотонные/запланированные будильники теряются. `BootReceiver`:
1. `goAsync()` → читает `BrewRepository`-aware источник (Koin уже поднят, т.к.
   `RoastiApplication.onCreate` отрабатывает до receiver'а) → `selectWaiting`.
2. Для каждого waiting Brew:
   - `waitUntil > now` → `scheduler.schedule(...)` заново;
   - `waitUntil <= now` → не планируем (wall-clock покажет «готово» при открытии).
     Опционально — сразу постить «готово»-уведомление для пропущенных. **Решение
     v1:** не постим, показываем при открытии. (Можно включить позже.)

### 6.4 Манифест и разрешения

Добавить в `composeApp/src/androidMain/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<!-- exact-alarm НЕ нужен: используем setAndAllowWhileIdle -->

<receiver android:name=".brew.BrewAlarmReceiver" android:exported="false" />
<receiver android:name=".brew.BootReceiver" android:exported="false">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

`POST_NOTIFICATIONS` (Android 13+) — рантайм-запрос при **первом** уходе шага в фон,
с rationale-объяснением. Если отказали — будильник всё равно ставим (тихо), статус
обновится по wall-clock при открытии; показываем in-app подсказку.

---

## 7. Навигация и интеграция с экраном шагов

### 7.1 Ключевой рефактор: экран шагов становится Brew-driven

Сейчас `RecipeStepsScreen` грузит **рецепт** по `recipeId` и создаёт `BrewingEngine`
(`RecipeStepsViewModel.kt:75`). Для персистентности/резюма источником шагов и позиции
должен стать **Brew**.

Новый маршрут:

```kotlin
Screen.Brew("brew/{brewId}")            // Routes.kt
// createRoute(brewId)
```

- Запуск из `RecipeItem` («Начать заваривать»): `brewRepository.startBrew(recipe)` →
  navigate `Screen.Brew(brew.id)`.
- `BrewViewModel(brewId)`:
  - подписка `brewRepository.observeById(brewId)`;
  - строит `BrewingEngine.fromRecipe(...)` из **снапшот-шагов** Brew, `startStep =
    currentStepIndex`;
  - на каждый advance движка → `brewRepository.advanceToStep(brewId, idx)` (персист);
  - на длинном шаге (>5 мин) показывает кнопку «Уведомить когда готово».

**Решено (§13.1):** полный переход на `brewId`. Старый маршрут
`recipe/{id}/steps/{startStep}` удаляется, `RecipeStepsRoute`/ViewModel мигрируют на
`Screen.Brew`. Все точки, что вели на `RecipeSteps.createRoute(...)`, переводятся на
`startBrew(recipe) → Screen.Brew(brew.id)`.

> Это самый крупный технический шов фичи. Foreground-`BrewingEngine` переиспользуется
> как есть (таймер шага), но «владелец позиции» теперь Brew. Engine можно даже не
> создавать на `waiting`-шаге — показываем экран ожидания.

### 7.2 Экран ожидания (waiting)

Отдельное состояние/composable внутри Brew-экрана:
- `now < waitUntil` → обратный отсчёт (`remainingMillis`), кнопка «Готово, продолжить»
  (= `resumeFromWait`, досрочно).
- `now >= waitUntil` → «Шаг завершён», кнопка «Продолжить» (= `resumeFromWait`).
- Тик отсчёта — `WallClock` + корутина (раз в секунду/минуту), это UI-only, не движок.

### 7.3 Deep-link из уведомления

`MainActivity` (`exported=true`, единственная) читает `intent.extras["brewId"]`:
- в `onCreate`/`onNewIntent` → навигация на `Screen.Brew(brewId)`.
- `singleTop`-launch у `contentIntent`, чтобы не плодить активити.
- Брать `brewId` из intent в стейт навигации (через nav callback / стартовый
  destination). Нет необходимости в `<deeplink>` URI — хватает extras + `onNewIntent`.

---

## 8. UI-слой (composeApp/androidMain)

```
ui/features/brew/
  carousel/
    ActiveBrewsSection.kt         — секция "Сейчас завариваются" (горизонт. карусель)
    ActiveBrewCardUiModel.kt
    mapper/ActiveBrewUiMapper.kt  — Brew → UiModel (countdown-строка, прогресс, imageUrl)
  waiting/  (часть Brew-экрана, §7.2)
  history/
    BrewHistoryScreen.kt          — экран "История"
    BrewHistoryViewModel.kt
  BrewViewModel.kt                — экран заваривания (§7.1)
```

### 8.1 Встройка карусели в Recipes

`RecipesListViewModel` (`RecipesListViewModel.kt:45`) получает `BrewRepository`,
добавляет:

```kotlin
val activeBrews: StateFlow<List<ActiveBrewCardUiModel>> =
    brewRepository.observeActive()
        .map { it.map(::toUiModel) }
        .stateInWhileSubscribe(emptyList())
```

`ActiveBrewsSection` рендерится **над** пагинированной лентой в `RecipesListScreen`
(скрыта при пустом списке). Карусель — `LazyRow`. Карточка ≠ recipe-карточка
(прогресс/таймер вместо лайков).

- `countdown`-строка форматируется в UI-маппере (display-готовая, как требует CLAUDE.md).
- `imageUrl` через существующий `ImageUrlBuilder.imageUrl(...)`.

### 8.2 Бейдж на табе Recipes

Счётчик `observeActive().map { it.size }` → бейдж в bottom bar на пункте Recipes.

### 8.3 Финал

После последнего шага — состояние «завершено» в Brew-экране: опциональное поле
заметки → `finishBrew(brewId, note)` → `COMPLETED` → навигация назад/в Историю.

---

## 9. DI (Koin)

Новые модули, порядок в `RoastiApplication.kt` (перед `viewModelsModule`):

```
platformModule → coreDatabaseModule → coreNetworkModule
  → *PagingModule
  → ... feature-модули ...
  → brewModule              (commonMain: BrewRepository, WallClock)
  → brewPlatformModule      (androidMain: BrewAlarmScheduler, BrewNotifier impl, AlarmManager, NotificationManager via androidContext())
  → viewModelsModule        ← всегда последний
```

ViewModel'и — только в `ViewModelsModule.kt`:
```kotlin
viewModel { params -> BrewViewModel(params.get(), get(), get(), get()) }   // brewId + deps
viewModel { BrewHistoryViewModel(get()) }
// RecipesListViewModel — добавить BrewRepository в существующую регистрацию
```

`brewPlatformModule` живёт в `shared/androidMain` (scheduler/notifier — рядом с
Paging-инфраструктурой, §13.2). Регистрация — в `RoastiApplication`.

---

## 10. Локализация

Все строки — `values/strings.xml` (en) + `values-ru/strings.xml` (ru), обе обязательны.
Набор (черновой): заголовок секции «Сейчас завариваются», «История», countdown-форматы
(«осталось %d ч», «%d мин»), кнопки «Уведомить когда готово» / «Готово, продолжить» /
«Продолжить» / «Отменить заваривание», контент уведомления, rationale разрешения,
empty-state истории, экран финала + плейсхолдер заметки. Финальные строки — на этапе
реализации.

---

## 11. Краевые случаи и риски

| Случай | Обработка |
|--------|-----------|
| Будильник не сработал (Doze/OEM/выкл) | wall-clock при открытии → «Готово, продолжить» (§2, PRODUCT §5) |
| Ребут телефона | `BootReceiver` перепланирует будущие, прошедшие показывает при открытии |
| Отказ в `POST_NOTIFICATIONS` | будильник ставим тихо, статус по wall-clock, in-app подсказка |
| Рецепт удалён/изменён во время Brew | снапшот шагов в Brew → не ломается |
| Несколько Brew готовы одновременно | у каждого свой будильник (request code = hash brewId), N уведомлений |
| `waitUntil` в момент выключенного телефона | при загрузке прошёл → готово при открытии |
| Картинка рецепта недоступна | placeholder, как в существующих карточках |
| Очень длинный список истории | без лимита в v1; TODO — чистка по `finished_at` |

---

## 12. Объём работ (статус)

1. ✅ **Данные**: `Brew.sq`, `BrewStepSnapshot.sq`, `BrewCacheMapper`, `WallClock`
   (+ `SystemWallClock`), доменные `Brew`/`BrewStatus`/`BrewStepSnapshot`.
2. ✅ **Репозиторий**: `BrewRepository` + `BrewRepositoryImpl` (commonMain), `BrewModule`,
   `BrewAlarmScheduler`-интерфейс.
3. ✅ **Платформа**: `BrewAlarmSchedulerImpl` + `BrewAlarmKeys` (shared/androidMain),
   `BrewNotifier`/`BrewAlarmReceiver`/`BootReceiver` + `brewPlatformModule`
   (composeApp/androidMain), манифест (+POST_NOTIFICATIONS, +RECEIVE_BOOT_COMPLETED,
   2 receiver'а), строки en/ru, регистрация в `RoastiApplication`.
   → собрано: `:composeApp:compileDebugKotlin` BUILD SUCCESSFUL.
4. ✅ **Навигация/экран** (4a + 4b готовы, компилируется):
   - ✅ `Screen.Brew("brew/{brewId}")`; старый `RecipeSteps`-маршрут удалён.
   - ✅ `BrewViewModel` (мост Brew DB ↔ foreground `BrewingEngine`: пересоздание движка только
     при входе в BREWING, персист advance через `advanceToStep`), `BrewUiState`.
   - ✅ `BrewScreen` (BREWING-рендер переисп. recipesteps-компоненты), `BrewWaitingScreen`
     (обратный отсчёт по wall-clock + «Продолжить»/«Готово, продолжить»/«Отменить»).
   - ✅ Кнопка «Уведомить когда готово» (на шагах >5мин) → `BackgroundDurationSheet` с
     `DurationWheelPicker` (дни/часы/мин на `WheelPicker`-примитиве; префилл из durationSeconds).
   - ✅ Старт: `RecipeContentViewModel.startBrewing` → `startBrew(recipe, startStep)` → nav `Brew`.
   - ✅ `BrewingEngine.fromSteps(...)` (рефактор `fromRecipe` → делегирует).
   - ✅ **4b**: deep-link — `MainActivity` читает `BrewAlarmKeys.EXTRA_BREW_ID` (onCreate/onNewIntent,
     `singleTop`) → `App`→`AppNavHost`→`MainNavHost` `LaunchedEffect(deepLinkBrewId)` → nav `Brew`
     (только когда Authenticated). Заметка на финале: `BrewingCompletionContent` + `OutlinedTextField`
     → `BrewViewModel.finish(note)` → `finishBrew(note)`. Countdown локализован (дн/ч/мин из ресурсов).
5. ✅ **UI Recipes**: `ActiveBrewsSection` (карусель «Сейчас завариваются» + ссылка «История →»)
   + `ActiveBrewCardUiModel`/маппер; `RecipesListViewModel.activeBrews` (observeActive →
   `stateInWhileSubscribe`); встройка в `RecipesListScreen` (item после фильтров). Бейдж активных
   на табе Recipes: `BrewBadgeViewModel` → `BottomBar.recipesBadgeCount` (BadgedBox).
6. ✅ **История**: `BrewHistoryScreen`/`BrewHistoryViewModel` (observeCompleted →
   `BrewHistoryItemUiModel` с `formatRelative` датой) + маршрут `Screen.BrewHistory` +
   ссылка «История →» из карусели.
7. ✅ **Финал**: экран завершения + опц. заметка → `finishBrew(note)` (сделано в 4b).
8. ⬜ **Тесты** (WallClock-fake: переходы статусов, isReady, resume). Строки en/ru уже
   добавлены для уведомлений; остальные — по мере UI.

> **Уточнение размещения (vs §13.2):** `shared/androidMain` НЕ содержит `koin-android`
> (нет `androidContext()`). Поэтому `brewPlatformModule` живёт в `composeApp/androidMain`
> (рядом с `platformModule`), а `BrewAlarmSchedulerImpl` остаётся в `shared/androidMain`
> (чистый Android, без koin). Ресиверы + `BrewNotifier` — в `composeApp/androidMain` ради
> доступа к `R.string`/`R.drawable`/`MainActivity`. Связь scheduler↔receiver — через
> action-string broadcast (`dev.roasti.action.BREW_READY`, package-scoped), без cross-module
> ссылок на классы.

---

## 13. Решённые технические вопросы

1. **Рефактор RecipeSteps → Brew-driven (§7.1)** — **да**, полный переход на `brewId`
   (`Screen.Brew("brew/{brewId}")`). Старый `recipe/{id}/steps/{startStep}`-маршрут
   уходит. Brew — единственный владелец позиции.
2. **`brewPlatformModule`** — в **`shared/androidMain`** (рядом с Paging-инфраструктурой).
3. **Обновление статуса** — **только при открытии** приложения (wall-clock — истина).
   `BrewAlarmReceiver` в БД не пишет, лишь постит уведомление.
4. **«Готово»-уведомление для брю, прошедших пока телефон был выключен** — **нет** в v1.
   `BootReceiver` их не постит; показываем «готово» при открытии.

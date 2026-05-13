# Roasti — KMP Coffee App

KMP-приложение для энтузиастов кофе: трекинг завариваний, рецепты, оценки. Android-first, iOS позже.

## Stack Android

- Kotlin, KMP
- UI: Jetpack Compose(Material3)
- DI: Koin 4.x
- DB: SQLDelight 2.x
- Async: Coroutines + Flow
-
    - Build: Gradle Version Catalogs (`gradle/libs.versions.toml`)

## Модули и правила размещения

| Модуль       | Source set    | Что туда идёт                                                                   |                                                                                                                                                               
|--------------|---------------|---------------------------------------------------------------------------------|
| `composeApp` | `androidMain` | Composable-экраны, ViewModel, навигация, тема                                   |
| `shared`     | `commonMain`  | Доменные модели, репозитории, сеть, DTO                                         |
| `shared`     | `androidMain` | Paging-медиаторы, SQLDelight driver, token storage, платформенные имплементации |
| `iosApp`     | `iosApp`      | Xcode проект, будет использовать SwiftUI + shared module. Пока не реализуем     |

**Нельзя**: `android.*` импорты в `commonMain`. Composable-функции в
`shared`.                                                                                                                        
**Нельзя**: доменные модели и репозитории в `composeApp`.

## Соглашения по именованию

| Сущность        | Паттерн                         | Пример               |
|-----------------|---------------------------------|----------------------|                                                                                                                                                                                         
| Response DTO    | `*Dto`                          | `PostResponseDto`    |
| Request DTO     | `*RequestDto`                   | `VoteRequestDto`     |                                                                                                                                                    
| Доменная модель | без суффикса                    | `Post`, `Recipe`     |
| Маппер          | `*Mapper`                       | `PostResponseMapper` |                                                                                                                                                         
| API клиент      | `*ApiClient` / `*ApiClientImpl` | `PostsApiClient`     |                                                                                                                                   
| Cache маппер    | `*CacheMapper`                  | `PostCacheMapper`    |
| Koin модуль     | `val *Module = module { }`      | `val postModule`     |

## Ключевые библиотеки

Версии в `gradle/libs.versions.toml`. Добавлять зависимость через TOML-алиас, не через строку.

| Библиотека | TOML-алиас                    | Заметка                              |                                                                                                                                                                 
|------------|-------------------------------|--------------------------------------|   
| Compose UI | `libs.compose.ui`             |                                      |                                                                                                                                                                  
| Material 3 | `libs.compose.material3`      |                                      |                                                                                                                                                           
| Navigation | `libs.navigation.compose`     | String routes, sealed class `Screen` |
| Koin       | `libs.koin.compose.viewmodel` |                                      |                                                                                                                                                            
| Ktor       | `libs.ktor.client.core`       | один HttpClient на всё приложение    |                                                                                                                                
| SQLDelight | `libs.sqldelight.coroutines`  | читать через Flow                    |                                                                                                                                     
| Coil       | `libs.coil.compose`           | AsyncImage, инит в RoastiApplication |                                                                                                                                 
| Paging     | `libs.paging.compose`         | только androidMain                   |

## Дизайн-токены и тема

Файлы: `composeApp/src/androidMain/.../ui/theme/`

| Что нужно   | Откуда брать                                                |                                                                                                                                                                          
|-------------|-------------------------------------------------------------|                                                                                                                                                                                           
| Цвета       | `MaterialTheme.colorScheme.*` или именованные из `Color.kt` |
| Отступы     | `RoastiSpacing.xs / sm / md / lg / xl`                      |                                                                                                                                                  
| Радиусы     | `RoastiShape.card / input / button`                         |
| Типографика | `MaterialTheme.typography.*`                                |                                                                                                                                                        

Значения для theme, colors, shapes, spacing, type - брать отсюда
`composeApp/src/androidMain/kotlin/dev/roasti/ui/theme/`
Бренд: Orange600 `#EA580C`, нейтралы: Stone-палитра (тёплые кофейные
тона).                                                                                                                           
Никогда не хардкодить `Color`, `sp` напрямую в Composable.

## Добавление новой фичи

Структура в `shared/commonMain/feature/<name>/`:

      domain/
        model/Foo.kt                    — чистый data class, без android/ktor импортов
        FooRepository.kt                — интерфейс (если у фичи есть domain-state или use-case'ы)
      data/
        network/FooApiClient.kt         — интерфейс + FooApiClientImpl
        remote/model/FooResponseDto.kt  — @Serializable
        mapper/FooMapper.kt             — Dto → domain model + SqlEntity → domain
        FooRepositoryImpl.kt
      di/
        FooModule.kt                    — Koin: single<FooRepository> { FooRepositoryImpl(get()) } bind FooRepository::class

    Не пропускать интерфейс репозитория, если фича имеет domain-state, use-case'ы или
    подменяемые реализации (mock vs rest). Для cross-cutting примитивов без domain-state
    (например, `LikesApiClient`) хватает интерфейса API-клиента — domain-репозиторий не нужен.

## DI

Порядок регистрации в `RoastiApplication.kt` (
соблюдать):                                                                                                                                             
platformModule → coreDatabaseModule → coreNetworkModule
→ *PagingModule → фича-модули → viewModelsModule ← всегда последний

Новый фича-модуль добавлять перед `viewModelsModule`.
ViewModel-ы объявлять только в `composeApp/di/ViewModelsModule.kt`:
`viewModel { FooViewModel(get()) }`                                                                                                                                                                   
Параметризованные: `viewModel { params -> FooViewModel(params.get(), get()) }`

## Пагинация (Paging 3 + SQLDelight)

Паттерн: RemoteMediator (API) → SQLDelight кэш → PagingSource (читает из
кэша).                                                                                                                       
Файлы живут в `shared/androidMain` (Paging — Android-only).

Референс:
`shared/src/androidMain/.../feature/post/data/paging/AllPostsRemoteMediator.kt`                                                                                                             
Koin-модуль: `shared/src/androidMain/.../core/di/PostPagingModule.kt`
SQLDelight `.sq` файлы: `shared/src/commonMain/sqldelight/dev/roasti/`

## Запрещено

- Хардкодить цвета, отступы, радиусы — использовать `RoastiSpacing`, `RoastiShape`, `MaterialTheme`
- Создавать новый `HttpClient` — брать из Koin (`get()`)
- Использовать Phosphor Icons (`PhosphorIcons.*`) - делать заглушку в виде composable Icon c
  дефолтной иконкой(`R.drawable.ic_chat`), добавить todo сверху, автор сам заменит на нужный
  painterResource
- Объявлять ViewModel в `shared/`
- Добавлять Koin-модуль без регистрации в `RoastiApplication`
- Пропускать интерфейс репозитория ради скорости

## Data(DataSource)

Все авторизированные запросы(в данный момент это все запросы) должны проходить через
`AuthorizedRequestExecutor`
Пример - `LikesApiClient` в
`shared/src/commonMain/kotlin/dev/roasti/feature/likes/data/LikesApiClient.kt`
Некоторые новые фичи требуют использования паралелльного ApiClient для моков. Смена реализаций rest
api и mock должна происходить в DI
Примеры: `shared/src/commonMain/kotlin/dev/roasti/feature/post/di/PostModule.kt`,
`shared/src/commonMain/kotlin/dev/roasti/feature/post/data/network/MockPostsApiClient.kt`
ApiClient возвращает Result<Dto>. RepositoryImpl маппит в Result<Domain>, ViewModel обрабатывает.
Data слой должен содержать два DataSource - remote и local(БД), они должны реализовывать 1 и тот же
интерфейс. Репозиторий будет заниматься их управлением
Репозиторий не должен знать как и откуда загружаются данные, он должен знать только тип - local,
remote.

### Два типа маппера

- `*ResponseMapper` — `FooDto` (сеть) → `Foo` (domain), живёт в `data/mapper/`
- `*CacheMapper` — SQLDelight-entity → `Foo` (domain), живёт в `data/mapper/`

## Паттерн: Repository

Репозиторий делает две вещи:

1. **Immutable StateFlow** — реактивное состояние (данные + статус), готовое для подписки из
   ViewModel
2. **Suspend-методы** — действия: загрузить, сохранить, удалить — возвращают `Result<T>`

  ```kotlin                                                                                                                                                                                             
  interface FooRepository {
    val state: StateFlow<FooState>          // подписка на живые данные                                                                                                                               
    suspend fun refresh(): Result<Unit>     // явное действие                                                                                                                                         
    suspend fun submit(data: Foo): Result<Unit>
}                                                                                                                                                                                                     
  ```                                                                                                                                                                                                 

StateFlow строится через `combine` +
`stateIn(scope, SharingStarted.Eagerly, initialValue)`.                                                                                                          
Референс: `shared/commonMain/.../feature/auth/data/AuthRepositoryImpl.kt`

### Что нельзя в domain-моделях

- Флаги загрузки, ошибки, UI-состояние (`isLoading`, `errorMessage`) — это UiState, не domain
- Android-импорты, аннотации Ktor/serialization
- Ссылки на ViewModel или репозиторий

### Когда sealed class в domain vs UiState в ViewModel

- Domain sealed class (`AuthState`, `SessionState`) — если состояние
  используется                                                                                                                     
  в нескольких фичах или в репозитории, и от этого может(или уже) зависить другая domain логика.
  Пример - `AuthState`
- `sealed interface *UiState` — если состояние нужно только одному экрану, живёт в ViewModel

## Паттерн: ViewModel

ViewModel подписывается на потоки из репозиториев, агрегирует их через `combine`,
отдаёт                                                                                                              
единственный immutable `val state: StateFlow<ScreenState>` для экрана.

  ```kotlin                                                                                                                                                                                           
  sealed interface FooUiState {
    data object Loading : FooUiState
    data class Error(val message: String) : FooUiState
    data class Content(val items: List<FooUiModel>, val isRefreshing: Boolean) : FooUiState
}

class FooViewModel(private val fooRepository: FooRepository) : ViewModel() {
    val state: StateFlow<FooUiState> = fooRepository.state
        .map { domainState -> domainState.toUiState() }
        .stateInWhileSubscribe(FooUiState.Loading)  // extension из utils/                                                                                                                            
}                                                                                                                                                                                                     
  ```

- Локальное UI-состояние (isRefreshing, dialog flags) — `private MutableStateFlow`
    - Агрегация из нескольких источников — `combine(...) { a, b -> ... }.stateInWhileSubscribe(...)`
    - Действия — `fun onFooClick() { viewModelScope.launch { ... } }`
    - Никогда не хранить `List` или данные напрямую в ViewModel — только через подписку на
      репозиторий
    - Domain модели никогда не должны проникать в Composable, должен быть маппер domain -> UiModel.
    - Мапер Живет в  `composeApp/.../ui/features/<name>/mapper/`. UiModel может содержать готовые
      строки,                                                                                                                  
      форматированные значения, display-флаги.

  Референс: `composeApp/.../ui/features/profile/ProfileViewModel.kt`

## Стратегия загрузки данных (единая для всех экранов)

**Поток на каждом экране:**
заход на экран → подписка на кэш (Flow из репозитория) → параллельно `refresh()` в API
→ ответ пишется в кэш → UI реактивно обновляется.

Кэш — единственный источник правды для UI. ViewModel/Composable никогда не читают данные
напрямую из API; репозиторий сам решает, когда сходить в сеть и когда отдать кэш.

### Состояния UI

| Сценарий                                | Поведение                                                |
|-----------------------------------------|----------------------------------------------------------|
| Кэша нет, идёт загрузка                 | fullscreen `LoadingStub`                                 |
| Кэша нет, ошибка от API                 | fullscreen `ErrorStub` с retry                           |
| Кэш есть, идёт фоновое обновление       | контент + `isRefreshing = true` (pull-to-refresh-indicator) |
| Кэш есть, ошибка (обычный экран)        | контент остаётся + snackbar через `UiEvent.ShowError`    |
| Кэш есть, ошибка (пагинированный экран) | контент + error-item в конце списка с retry              |

Общий sealed для большинства экранов:
`ContentUiState<T> = Loading | FullscreenError(error) | Content(data, isRefreshing)`.
Живёт в `ui/uikit/state/`.

Для пагинации: ошибка `LoadState.Append.Error` → отдельный composable `PagingErrorItem`
в `LazyColumn`/`LazyVerticalGrid` через `items.loadState.append`.

### Optimistic update

Пишем сразу в локальный кэш (БД) → API-вызов → на ошибке откатываем кэш и показываем
snackbar. Никогда не обновляем UI «в обход кэша» — оба пути должны идти через одну
точку (`*RepositoryImpl` + БД), чтобы все подписчики реагировали одинаково.

### Исключение: поисковые фичи

Поиск рецептов, поиск постов и подобные query-driven фичи **не кэшируются** — данные
отображаются строго из сети. Кэш бесполезен: каждый запрос уникален, результаты быстро
устаревают, хранить нечего.

Состояния для поиска:

| Сценарий            | Поведение                                |
|---------------------|------------------------------------------|
| Идёт загрузка       | inline-`LoadingStub` в области результатов |
| Ошибка от API       | inline-`ErrorStub` с retry в области результатов |
| Пустой результат    | inline empty-state                       |

Для пагинированного поиска — `RemoteRecipesPagingSource` без `RemoteMediator` и без
записи в БД. Референс: `PagingRecipeRepository.getRemoteSearchPager`.

## Фича: Рецепты (центральная фича приложения)

Объединяет три use-case'а — feed, favorites, recipe details — поверх **одной сущности и одного кэша**.

### Архитектура

```
shared/commonMain/feature/recipe/
  domain/
    RecipeRepository                — single recipe: observeById/refresh/CRUD/toggleLike
    model/Recipe, RecipeDraft, RecipesPagingQuery, ...
  data/
    network/RecipesApiClient        — interface + Impl
    remote/model/*Dto
    mapper/                         — Dto → domain, SqlEntity → domain
    RecipeRepositoryImpl(api, db, likesApi)
    RecipeListType                  — константы 'feed' | 'favorites'

shared/commonMain/feature/likes/
  data/network/LikesApiClient       — interface + Impl, примитив для /likes endpoints
  data/*Dto, *Mappers
  domain/model/                     — LikedRecipe, RecipeLike
  ⛔ нет LikesRepository — для thin HTTP-обёртки domain-репо не нужен

shared/androidMain/feature/recipe/
  domain/RecipeListsRepository      — все ленты: observeFeed/Favorites/Search + observeHasCachedFeed
  data/RecipeListsRepositoryImpl(db, recipesApi, auth, favMediator)
  data/paging/
    AllRecipesRemoteMediator        — feed
    FavoritesRemoteMediator         — favorites (через LikesApiClient)
    RemoteRecipesPagingSource       — search (без кэша)

shared/commonMain/sqldelight/
  Recipe.sq                         — сущность (id, title, is_liked, likes_count, ...)
  RecipeStep.sq                     — шаги, связаны через recipe_id
  RecipeListMembership.sq           — junction (list_type, recipe_id, position)
  RecipeRemoteKey.sq                — пагинация (по list_type)
```

### Ключевая идея: один Recipe + membership на каждую ленту

`Recipe` — единственная entity-таблица. `RecipeListMembership(list_type, recipe_id, position)` —
кто в каком списке и в каком порядке (`position` берётся со стороны сервера). Каждая лента
живёт в своём срезе membership, обе JOIN'ятся к одной `Recipe`-таблице.

- **Изоляция**: REFRESH `'feed'` не трогает membership('favorites') и наоборот.
- **Реактивная согласованность**: `is_liked` хранится один раз. Лайк → один UPDATE на Recipe →
  оба списка перерисуются автоматически через Flow.
- **Никакого дубля данных** между лентами.

Новая лента — это новый `list_type` + новый mediator. Без новой таблицы.

### toggleLike (optimistic, единственная точка)

`RecipeRepository.toggleLike(id)` — только тут можно лайкать. В одной транзакции:

1. `Recipe.toggleLike` (`is_liked = 1 - is_liked`, `±likes_count`)
2. `RecipeListMembership('favorites', id, ...)` — insert at top (position = MIN−1) или delete
3. `LikesApiClient.toggleLikeOnRecipe` — сетевой вызов
4. На ошибке откат обоих шагов (`applyLikeToggle(!wasLiked)`) и `Result.failure`

Подписчики обоих списков (feed + favorites) видят и оптимистичный apply, и revert через одну
Flow-цепочку — без events/eventbus между экранами.

### Кто за что отвечает

| Класс                       | Source set | Ответственность                                                  |
|-----------------------------|------------|------------------------------------------------------------------|
| `RecipeRepository`          | commonMain | Единичный рецепт + toggleLike                                    |
| `RecipeListsRepository`     | androidMain | Pager-flow'ы для feed / favorites / search                      |
| `LikesApiClient`            | commonMain | HTTP-обёртка `/likes` endpoints (примитив, без domain)           |
| `AllRecipesRemoteMediator`  | androidMain | feed → `/recipes` → Recipe + membership('feed')                 |
| `FavoritesRemoteMediator`   | androidMain | favorites → `/users/{id}/likes` → Recipe + membership('favorites') |
| `RemoteRecipesPagingSource` | androidMain | search — без кэша, без mediator                                 |

### Что нельзя

- Создавать ещё одну entity-таблицу для нового списка рецептов. Новая лента = новый `list_type`
  в membership.
- Писать в `Recipe`-таблицу из ViewModel или UI напрямую — только через `RecipeRepository`.
- Дублировать toggleLike-логику. Любое место, которое лайкает рецепт, идёт через
  `RecipeRepository.toggleLike`.
- Кэшировать поиск (см. «Стратегия загрузки данных → Исключение»).

## Локализация

Все строки — только в ресурсных файлах. Никаких строк напрямую в Composable.

- `composeApp/src/androidMain/res/values/strings.xml` — английский (дефолт)
- `composeApp/src/androidMain/res/values-ru/strings.xml` — русский

Обе локали обязательны при добавлении новой строки. iOS-локализация пока не нужна.

  ```xml                                                                                                                                                                                                
  <!-- strings.xml (en) -->
<string name="feed_title">Feed</string>

    <!-- values-ru/strings.xml -->
<string name="feed_title">Лента</string>                                                                                                                                                              
  ```

В Compose: `stringResource(R.string.feed_title)`

## Навигация

Routes: `composeApp/.../navigation/Routes.kt` — sealed class `Screen`.

Текущее дерево:

  ```                                                                                                                                                                                                 
  Login → Register                                                                                                                                                                                      
        → Main                                                                                                                                                                                        
            ├─ Feed
            ├─ Recipes → RecipeItem(id) → RecipeSteps(id, startStep)                                                                                                                                    
            │                           → EditRecipe(id)                                                                                                                                                
            │  CreateRecipe                                                                                                                                                                             
            └─ Profile → Settings                                                                                                                                                                       
  ```                                                                                                                                                                                                   

Bottom bar отображается только на экранах из `bottomNavScreens` (Feed, Recipes,
Profile).                                                                                                             
Переходы: `NavTransitions.kt`. При добавлении экрана — добавить composable{} в `AppNavHost.kt`.

## Загрузка изображений(Coil)

Во viewmodel ссылки на изображенния попадают из бекенда в виде обычных id, для того чтобы собрать
корректный url,
используй - fun `imageUrl` в `ImageUrlBuilder.kt`, построение должно быть на уровне viewModel или ui
маппера.
Пример в
`composeApp/src/androidMain/kotlin/dev/roasti/ui/features/feed/mapper/PostUiMapper.kt`

## UI(Compose)

Общие UI компоненты находятся в ui kit -
`composeApp/src/androidMain/kotlin/dev/roasti/ui/uikit`
При построении нового ui предпочтительно переиспользовать существующие и добавлять новые компоненты.
Новый компонент должен быть переиспользуемым, гибким, удобным для использования разными фичами, а
поэтому универсален
Уточни у пользователя, нужно ли добавлять компонент, или нет

### Подписка на StateFlow в Composable

`val state by viewModel.state.collectAsStateWithLifecycle()`
Не использовать `collectAsState()` — нет lifecycle-aware поведения.

### Структура экрана

  ```kotlin
  // FooScreen.kt — только иньекция VM
@Composable
fun FooScreen(viewModel: FooViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (state) {
        is Loading -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            LoadingStub(Modifier.align(Alignment.Center))
        }

        is Error -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            ErrorStub(...)
        }

        is Content -> FooContent(state, listener)
    }
}

// FooContent — чистый composable, принимает state + listener-интерфейс
@Composable
fun FooContent(state: FooUiState, listener: FooListener) {
    ...
}
  ```

### Event listener-интерфейс

Действия экрана — через интерфейс, который реализует ViewModel:

  ```kotlin
  interface FooListener {
    fun onItemClick(id: String)
    fun onRefresh()
}
  ```

Не передавать лямбды по одной в Composable экрана.
Если в Composable виджете 2 или меньше лямбды - не использовать отдельный интерфейс, если больше -
использовать

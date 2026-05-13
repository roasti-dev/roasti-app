# Architecture

## Goal

Organize `shared` so feature code stays localized, navigation across the codebase is simpler, and future split into Gradle feature modules is mostly mechanical.

Target approach:

- `core` for shared infrastructure and cross-feature contracts
- `feature/<name>` for feature-specific code
- each feature owns its own `data`, `domain`, `presentation`, and `di`

## Target Structure

```text
shared/src/commonMain/kotlin/dev/roasti/
  core/
    database/
    di/
    network/
    utils/

  feature/
    auth/
      data/
      domain/
      presentation/
      di/

    recipe/
      data/
      domain/
      presentation/
      di/

    likes/
      data/
      domain/
      presentation/
      di/

    upload/
      data/
      domain/
      presentation/
      di/
```

## Core Vs Feature

Put code into `core` only if at least one of these is true:

- infrastructure used by multiple features
- shared technical code with no feature ownership
- cross-feature contracts that should not belong to one feature

Examples for `core`:

- `HttpClientFactory`
- authorized request executor
- database driver and database setup
- generic image/url/file helpers
- shared DI bootstrap or module aggregation
- interfaces used by several features and not owned by one feature

Put code into `feature/<name>` if the code exists only because that feature exists.

Examples:

- repositories and data sources for one feature
- DTOs and mappers for one feature API
- domain models used only by one feature
- view models, stores, ui state, feature presentation logic
- feature-specific DI module

## Feature Structure

Each feature should follow one layout.

```text
feature/<name>/
  data/
    local/
    network/
    mapper/
    model/
    repository/

  domain/
    model/
    repository/
    usecase/

  presentation/
    model/
    state/
    <screens or stores>

  di/
    <FeatureName>Module.kt
```

Not every feature must have every subfolder. Create only what is needed.

Preferred rule:

- `data` depends on `domain` contracts
- `presentation` depends on `domain`
- `domain` does not depend on `data` or `presentation`

## DI Rules

Each feature exposes its own Koin module.

Example:

```kotlin
val recipeModule = module {
    single<RecipeRepository> { NetworkRecipeRepository(get()) }
    factory<RecipeFilterStore> { RecipeFilterStore() }
}
```

Shared infrastructure is registered in dedicated core modules.

Example:

```kotlin
val coreNetworkModule = module {
    single {
        createHttpClient(
            accessTokenProvider = { get<SessionRepository>().currentSession()?.accessToken }
        )
    }
}
```

Koin should still be assembled in one composition root.

Example:

```kotlin
startKoin {
    modules(
        coreDatabaseModule,
        coreNetworkModule,
        authModule,
        uploadModule,
        likesModule,
        recipeModule,
    )
}
```

### DI Principles

- each feature registers only its own dependencies
- shared infrastructure is registered in `core`
- app remains the place that combines all modules
- features must request interfaces, not concrete implementations from other features

## Feature-To-Feature Dependencies

Allowed:

- feature -> core
- feature -> feature contract

Not allowed:

- feature -> another feature implementation
- feature -> another feature DTOs, mappers, api clients, data sources

If feature `recipe` needs image upload, `recipe` should depend on `UploadRepository`, not on `NetworkUploadRepository`.

## When To Use Api And Impl Modules

Use `api/impl` only when one feature provides capability for another feature.

Example:

- `:feature:upload:api`
- `:feature:upload:impl`

`api` contains:

- public contracts
- domain models needed outside the feature
- use cases that other features may consume

`impl` contains:

- repository implementations
- api clients
- DTOs
- mappers
- local storage
- DI bindings

Example:

```kotlin
val uploadModule = module {
    single<UploadRepository> { NetworkUploadRepository(get(), get()) }
}
```

Another feature depends only on `UploadRepository` from `api`. Real implementation is provided at runtime by adding `uploadModule` in app Koin setup.

## Rules For New Code

When adding a new feature:

1. Create `feature/<name>/`
2. Add only the folders needed for current scope
3. Put feature-specific models and repository contracts inside the feature
4. Put the feature DI module in `feature/<name>/di`
5. Add the feature Koin module to the app composition root
6. Move code to `core` only if it is truly shared

Do not create new top-level global layer folders like:

- `data/<feature>`
- `domain/<feature>`
- `presentation/<feature>`

All new feature code should start inside `feature/<name>`.

## Migration Rule

Migration should be incremental.

Recommended order:

1. move shared infrastructure to `core`
2. split DI into feature and core modules
3. migrate features one by one
4. clean up cross-feature dependencies
5. remove obsolete old packages

Do not migrate several large features at once.

## Checklist For Splitting A Feature Into A Gradle Module

1. Ensure feature code is already isolated in `feature/<name>`
2. Ensure other features depend only on contracts, not implementations
3. Move feature package into a dedicated Gradle module
4. Keep feature DI module inside that Gradle module
5. Add the feature module dependency where needed
6. Register the feature Koin module in the app composition root

If a feature is reused by another feature, consider:

- `:feature:<name>:api`
- `:feature:<name>:impl`

Use this split only when there is real cross-feature reuse.

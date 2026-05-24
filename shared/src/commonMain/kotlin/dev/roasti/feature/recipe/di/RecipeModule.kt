package dev.roasti.feature.recipe.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import dev.roasti.feature.recipe.data.RecipeRepositoryImpl
import dev.roasti.feature.recipe.data.network.RecipesApiClient
import dev.roasti.feature.recipe.data.network.RecipesApiClientImpl
import dev.roasti.feature.recipe.domain.RecipeRepository
import dev.roasti.feature.recipe.domain.session.BrewingClock
import dev.roasti.feature.recipe.domain.session.BrewingClockImpl
import dev.roasti.feature.recipe.presentation.filter.RecipeFilterStore

val recipeModule = module {
    single { RecipesApiClientImpl(get(), get()) } bind RecipesApiClient::class
    single { RecipeRepositoryImpl(get(), get(), get()) } bind RecipeRepository::class
    single<BrewingClock> { BrewingClockImpl() }
    factory { RecipeFilterStore() }
}

package dev.roasti.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import dev.roasti.navigation.AppNavigationViewModel
import dev.roasti.ui.features.auth.login.LoginViewModel
import dev.roasti.ui.features.auth.register.RegisterViewModel
import dev.roasti.ui.features.createrecipe.CreateRecipeScreenViewModel
import dev.roasti.ui.features.createrecipe.CreateRecipeViewModel
import dev.roasti.ui.features.editrecipe.EditRecipeViewModel
import dev.roasti.ui.features.favorites.FavoritesViewModel
import dev.roasti.ui.features.feed.FeedViewModel
import dev.roasti.ui.features.postcompose.PostComposeViewModel
import dev.roasti.ui.features.postdetail.PostDetailViewModel
import dev.roasti.ui.features.profile.ProfileViewModel
import dev.roasti.ui.features.recipelist.RecipesListViewModel
import dev.roasti.ui.features.recipepage.RecipeContentViewModel
import dev.roasti.ui.features.recipesteps.RecipeStepsViewModel
import dev.roasti.ui.features.userprofile.UserProfileViewModel

val viewModelsModule = module {
    viewModel { AppNavigationViewModel(get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { RegisterViewModel(get()) }
    viewModel { ProfileViewModel(get(), get(), get()) }
    viewModel { RecipesListViewModel(get(), get(), get(), get(), get()) }
    viewModel { FeedViewModel(get(), get()) }
    viewModel { params -> PostDetailViewModel(params.get(), get(), get(), get()) }
    viewModel { params ->
        PostComposeViewModel(
            postId = params.getOrNull<String>(),
            pagingPostRepository = get(),
            uploadRepository = get(),
        )
    }
    viewModel { params -> RecipeContentViewModel(params.get(), get()) }
    viewModel { params -> RecipeStepsViewModel(params.get(), params.get(), get(), get()) }
    viewModel { CreateRecipeViewModel(get(), get()) }
    viewModel { CreateRecipeScreenViewModel(get(), get()) }
    viewModel { params -> EditRecipeViewModel(params.get(), get(), get()) }
    viewModel { FavoritesViewModel(get(), get()) }
    viewModel { params -> UserProfileViewModel(params.get(), params.get(), get()) }
}

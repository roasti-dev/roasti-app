package dev.roasti.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val SlideDurationMillis = 250
private const val FadeDurationMillis = 150

private val tabRoutes: List<String> = bottomNavScreens.map { it.route }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean {
    val from = initialState.destination.route
    val to = targetState.destination.route
    return from in tabRoutes && to in tabRoutes
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabSlideDirection(): SlideDirection {
    val fromIdx = tabRoutes.indexOf(initialState.destination.route)
    val toIdx = tabRoutes.indexOf(targetState.destination.route)
    return if (toIdx >= fromIdx) SlideDirection.Left else SlideDirection.Right
}

/**
 * Telegram-style horizontal slide between bottom-nav tabs; fade for any other transition
 * (e.g. tab → detail screen). Direction follows the order in [bottomNavScreens].
 */
fun NavGraphBuilder.tabComposable(
    route: String,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable(
        route = route,
        enterTransition = { tabEnterOrFade() },
        exitTransition = { tabExitOrFade() },
        popEnterTransition = { tabEnterOrFade() },
        popExitTransition = { tabExitOrFade() },
        content = content,
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabEnterOrFade(): EnterTransition =
    if (isTabSwitch()) {
        slideIntoContainer(tabSlideDirection(), tween(SlideDurationMillis))
    } else {
        fadeIn(tween(FadeDurationMillis))
    }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabExitOrFade(): ExitTransition =
    if (isTabSwitch()) {
        slideOutOfContainer(tabSlideDirection(), tween(SlideDurationMillis))
    } else {
        fadeOut(tween(FadeDurationMillis))
    }

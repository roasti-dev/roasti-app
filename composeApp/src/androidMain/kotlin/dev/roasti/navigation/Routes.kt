package dev.roasti.navigation

// 🔑 KMP: string routes work in commonMain without extra dependencies.
// Later upgrade path: @Serializable objects (type-safe nav) — needs kotlinx-serialization plugin.

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Feed : Screen("feed")
    object Recipes : Screen("recipes")
    object RecipeItem : Screen("recipe/{id}") {
        fun createRoute(id: String) = "recipe/$id"
    }

    object RecipeSteps : Screen("recipe/{id}/steps/{startStep}") {
        fun createRoute(id: String, startStep: Int = 0) = "recipe/$id/steps/$startStep"
    }

    object EditRecipe : Screen("recipe/{id}/edit") {
        fun createRoute(id: String) = "recipe/$id/edit"
    }

    object CreateRecipe : Screen("recipe/create")

    object Profile : Screen("profile")

    object Favorites : Screen("favorites")

    object Settings : Screen("settings")

    object UserProfile : Screen("user/{userId}/{username}?avatarTag={avatarTag}") {
        const val ARG_USER_ID = "userId"
        const val ARG_USERNAME = "username"
        const val ARG_AVATAR_TAG = "avatarTag"
        fun createRoute(userId: String, username: String, avatarTag: String? = null): String {
            val base = "user/$userId/${android.net.Uri.encode(username)}"
            return if (avatarTag != null) {
                "$base?avatarTag=${android.net.Uri.encode(avatarTag)}"
            } else {
                base
            }
        }
    }

    object PostDetail : Screen("post/{id}") {
        const val ARG_ID = "id"
        fun createRoute(id: String) = "post/$id"
    }

    object PostCompose : Screen("post/compose?postId={postId}") {
        const val ARG_POST_ID = "postId"
        fun createRoute(postId: String? = null): String =
            if (postId == null) "post/compose" else "post/compose?postId=$postId"
    }

    object PhotoViewer : Screen("photo/viewer?images={images}&initialIndex={initialIndex}") {
        const val ARG_IMAGES = "images"
        const val ARG_INITIAL_INDEX = "initialIndex"
        private const val SEPARATOR = "|"
        fun createRoute(images: List<String>, initialIndex: Int = 0): String {
            val joined = images.joinToString(SEPARATOR) { android.net.Uri.encode(it) }
            return "photo/viewer?images=$joined&initialIndex=$initialIndex"
        }
        fun parseImages(raw: String?): List<String> = raw
            ?.split(SEPARATOR)
            ?.mapNotNull { android.net.Uri.decode(it).takeIf { decoded -> decoded.isNotEmpty() } }
            .orEmpty()
    }
}

// Screens that show the bottom navigation bar
val bottomNavScreens = listOf(Screen.Feed, Screen.Recipes, Screen.Profile)

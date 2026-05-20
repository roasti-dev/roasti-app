package dev.roasti.core.network

private const val ApiPath = "/api/v1"

object ApiRoutes {
    val users = UserRoutes

    const val AuthPathPrefix = "$ApiPath/auth/"
    const val Login = "${AuthPathPrefix}login"
    const val Register = "${AuthPathPrefix}register"
    const val Logout = "${AuthPathPrefix}logout"
    const val Refresh = "${AuthPathPrefix}refresh"
    const val Recipes = "$ApiPath/recipes"
    fun recipeById(id: String) = "$Recipes/$id"
    const val UploadsImages = "$ApiPath/uploads/images"

    fun recipeLike(recipeId: String) = "$Recipes/$recipeId/like"
    fun userLikedRecipes(userId: String) = "${users.prefix}/$userId/likes"

    const val Posts = "$ApiPath/posts"
    fun postById(id: String) = "$Posts/$id"
    fun postVote(id: String) = "$Posts/$id/vote"
    fun postComments(postId: String) = "$Posts/$postId/comments"
    fun commentById(commentId: String) = "$ApiPath/comments/$commentId"
}

object UserRoutes {
    const val prefix = "$ApiPath/users"
    const val me = "$prefix/me"
    fun byUsername(username: String) = "$prefix/$username"
}

object NetworkHeaders {
    const val BearerPrefix = "Bearer "
}

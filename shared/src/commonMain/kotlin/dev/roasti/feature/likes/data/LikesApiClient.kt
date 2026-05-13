package dev.roasti.feature.likes.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import dev.roasti.core.network.ApiRoutes
import dev.roasti.core.network.AuthorizedRequestExecutor
import dev.roasti.core.network.PageResponseDto

private const val LikedRecipesTypeQueryParameter = "type"
private const val LimitQueryParameter = "limit"
private const val PageQueryParameter = "page"
private const val RecipeLikeType = "recipe"

interface LikesApiClient {
    suspend fun getLikedRecipes(
        userId: String,
        limit: Int = 50,
        page: Int = 1,
    ): Result<PageResponseDto<LikedRecipeItemDto>>

    suspend fun toggleLikeOnRecipe(recipeId: String): Result<RecipeLikeDto>
}

class LikesApiClientImpl(
    private val httpClient: HttpClient,
    private val authorizedRequestExecutor: AuthorizedRequestExecutor,
) : LikesApiClient {
    override suspend fun getLikedRecipes(
        userId: String,
        limit: Int,
        page: Int,
    ): Result<PageResponseDto<LikedRecipeItemDto>> = authorizedRequestExecutor.execute {
        httpClient.get(ApiRoutes.userLikedRecipes(userId)) {
            url {
                parameters.append(LikedRecipesTypeQueryParameter, RecipeLikeType)
                parameters.append(LimitQueryParameter, limit.toString())
                parameters.append(PageQueryParameter, page.toString())
            }
        }.body<PageResponseDto<LikedRecipeItemDto>>()
    }

    override suspend fun toggleLikeOnRecipe(recipeId: String): Result<RecipeLikeDto> =
        authorizedRequestExecutor.execute {
            return@execute httpClient.post(ApiRoutes.recipeLike(recipeId)).body<RecipeLikeDto>()
        }
}

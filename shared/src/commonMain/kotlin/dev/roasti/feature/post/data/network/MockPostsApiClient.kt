package dev.roasti.feature.post.data.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import dev.roasti.feature.post.data.mapper.toDomain
import dev.roasti.feature.post.data.remote.model.VoteDirectionDto
import dev.roasti.feature.post.data.remote.model.request.CreatePostRequestDto
import dev.roasti.feature.post.data.remote.model.request.UpdatePostRequestDto
import dev.roasti.feature.post.data.remote.model.request.VoteRequestDto
import dev.roasti.feature.post.data.remote.model.response.PostAuthorDto
import dev.roasti.feature.post.data.remote.model.response.PostRecipeRefDto
import dev.roasti.feature.post.data.remote.model.response.PostRecipeStatusDto
import dev.roasti.feature.post.data.remote.model.response.PostResponseDto
import dev.roasti.feature.post.data.remote.model.response.PostVoteResponseDto
import dev.roasti.core.network.PageResponseDto
import dev.roasti.core.network.PaginationResponseDto
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private const val SimulatedLatencyMillis = 300L
private const val MockPostsCount = 50

internal val CurrentMockAuthor: PostAuthorDto = PostAuthorDto(
    id = "u_current",
    username = "u/you",
    avatarId = "https://picsum.photos/seed/avatar_current/96/96",
)

class MockPostsApiClient(
    private val currentAuthorProvider: () -> PostAuthorDto = { CurrentMockAuthor },
) : PostsApiClient {

    private val mutex = Mutex()
    private val posts: MutableList<PostResponseDto> = generateMockPosts().toMutableList()
    private var failNextRequest = false

    /** Toggle: the next API call (any method) returns Result.failure. Useful for testing rollback. */
    fun simulateNextFailure() {
        failNextRequest = true
    }

    override suspend fun getPosts(
        page: Int,
        limit: Int,
    ): Result<PageResponseDto<PostResponseDto>> {
        delay(SimulatedLatencyMillis)
        if (consumeFailureFlag()) return Result.failure(IllegalStateException("Simulated failure"))

        return mutex.withLock {
            val total = posts.size
            val lastPage = max(1, ceil(total.toDouble() / limit).toInt())
            val safePage = page.coerceAtLeast(1)
            val from = (safePage - 1) * limit
            val to = min(from + limit, total)
            val items = if (from in 0..<total) posts.subList(from, to) else emptyList()
            val nextPage = if (safePage < lastPage) safePage + 1 else safePage

            Result.success(
                PageResponseDto(
                    items = items,
                    pagination = PaginationResponseDto(
                        currentPage = safePage,
                        itemsCount = total,
                        lastPage = lastPage,
                        nextPage = nextPage,
                    ),
                )
            )
        }
    }

    override suspend fun getPost(id: String): Result<PostResponseDto> {
        delay(SimulatedLatencyMillis)
        if (consumeFailureFlag()) return Result.failure(IllegalStateException("Simulated failure"))

        return mutex.withLock {
            posts.firstOrNull { it.id == id }
                ?.let(Result.Companion::success)
                ?: Result.failure(NoSuchElementException("Post $id not found"))
        }
    }

    override suspend fun createPost(request: CreatePostRequestDto): Result<PostResponseDto> {
        delay(SimulatedLatencyMillis)
        if (consumeFailureFlag()) return Result.failure(IllegalStateException("Simulated failure"))

        return mutex.withLock {
            val now = Clock.System.now()
            val newId = "post_local_${now.toEpochMilliseconds()}"
            val dto = PostResponseDto(
                id = newId,
                author = currentAuthorProvider(),
                title = request.title,
                text = request.text.orEmpty(),
                images = request.images,
                recipe = request.recipeId?.let {
                    PostRecipeRefDto(id = it, status = PostRecipeStatusDto.AVAILABLE)
                },
                rating = 0,
                userVote = VoteDirectionDto.NONE,
                commentsCount = 0,
                createdAt = now,
                updatedAt = now,
            )
            posts.add(0, dto)
            Result.success(dto)
        }
    }

    override suspend fun updatePost(
        id: String,
        request: UpdatePostRequestDto,
    ): Result<PostResponseDto> {
        delay(SimulatedLatencyMillis)
        if (consumeFailureFlag()) return Result.failure(IllegalStateException("Simulated failure"))

        return mutex.withLock {
            val index = posts.indexOfFirst { it.id == id }
            if (index < 0) {
                return@withLock Result.failure(NoSuchElementException("Post $id not found"))
            }
            val current = posts[index]
            val updated = current.copy(
                title = request.title,
                text = request.text.orEmpty(),
                images = request.images,
                recipe = request.recipeId?.let {
                    PostRecipeRefDto(id = it, status = PostRecipeStatusDto.AVAILABLE)
                },
                updatedAt = Clock.System.now(),
            )
            posts[index] = updated
            Result.success(updated)
        }
    }

    override suspend fun deletePost(id: String): Result<Unit> {
        delay(SimulatedLatencyMillis)
        if (consumeFailureFlag()) return Result.failure(IllegalStateException("Simulated failure"))

        return mutex.withLock {
            posts.removeAll { it.id == id }
            Result.success(Unit)
        }
    }

    override suspend fun vote(
        id: String,
        request: VoteRequestDto,
    ): Result<PostVoteResponseDto> {
        delay(SimulatedLatencyMillis)
        if (consumeFailureFlag()) return Result.failure(IllegalStateException("Simulated failure"))

        return mutex.withLock {
            val index = posts.indexOfFirst { it.id == id }
            if (index < 0) {
                return@withLock Result.failure(NoSuchElementException("Post $id not found"))
            }
            val current = posts[index]
            val previous = current.userVote.toDomain()
            val target = request.type.toDomain()
            val delta = previous.deltaTo(target)
            val updated = current.copy(
                rating = current.rating + delta,
                userVote = request.type,
            )
            posts[index] = updated
            Result.success(
                PostVoteResponseDto(
                    rating = updated.rating,
                    userVote = updated.userVote,
                )
            )
        }
    }

    private fun consumeFailureFlag(): Boolean {
        if (!failNextRequest) return false
        failNextRequest = false
        return true
    }
}

private fun generateMockPosts(): List<PostResponseDto> {
    val now: Instant = Clock.System.now()
    val authors = listOf(
        PostAuthorDto(id = "u1", username = "u/sarah_j", avatarId = "https://picsum.photos/seed/avatar_1/96/96"),
        PostAuthorDto(id = "u2", username = "u/marcus_brews", avatarId = "https://picsum.photos/seed/avatar_2/96/96"),
        PostAuthorDto(id = "u3", username = "u/coffee_lab", avatarId = "https://picsum.photos/seed/avatar_3/96/96"),
        PostAuthorDto(id = "u4", username = "u/single_origin", avatarId = "https://picsum.photos/seed/avatar_4/96/96"),
        PostAuthorDto(id = "u5", username = "u/dark_roast", avatarId = "https://picsum.photos/seed/avatar_5/96/96"),
    )
    val texts = listOf(
        "Dialing in the new Ethiopian Yirgacheffe this morning\nThe floral notes are absolutely singing with a slightly coarser grind and a lower water temp (around 92°C). Definitely worth the extra few minutes of prep, the cup ends up bright and clean with a long, syrupy finish.",
        "Finally perfected my summer iced latte ratio!\nThe secret is flash-chilling the espresso immediately over a large ice sphere before adding the oat milk. Keeps the texture creamy without watering it down. Full recipe linked below.",
        "Weekend cupping notes\nThree Kenyans side by side, all washed process. The standout was a Nyeri lot with massive blackcurrant and a tomato-vine acidity that lingered for minutes.",
        "Chemex vs V60 for naturals\nSpent the morning A/B testing the same Brazilian natural in both. V60 brought out the chocolate and nuttiness, Chemex was cleaner but lost some body. Both have their place.",
        "First time roasting at home\nPicked up a SR540 last week. The learning curve is real but the difference between a 5-day-rest and a 10-day-rest is night and day. Patience pays.",
        "Espresso shot pulling slow today\nGrinder finally needs a deep clean. Going to take the burrs out tonight and post pics of how grim it looks in there.",
        "Best decaf I've ever had\nA Colombian sugarcane decaf from a small roaster in Berlin. Honestly couldn't tell it was decaf in a blind tasting.",
        "Latte art practice update\nMonth three of pouring daily. Finally getting consistent rosettas with whole milk. Oat is still humbling me.",
        "Cold brew concentrate ratio\nLanded on 1:5 coffee to water by weight, 18 hours at room temp, then filtered twice. Cuts perfectly with milk or sparkling water.",
        "Coffee cherry tea (cascara)\nFirst time trying it. Tastes like hibiscus crossed with raisin. Definitely buying more next time I see it.",
    )
    val photoIds = listOf(
        null,
        "https://picsum.photos/seed/post_1/800/500",
        "https://picsum.photos/seed/post_2/800/500",
        null,
        "https://picsum.photos/seed/post_3/800/500",
        null,
        "https://picsum.photos/seed/post_4/800/500",
        null,
        "https://picsum.photos/seed/post_5/800/500",
        null,
    )
    val recipeIds = listOf(null, "recipe_iced_latte", null, null, null, null, null, null, "recipe_cold_brew", null)

    return List(MockPostsCount) { i ->
        val ageHours = i * 2L + 1
        val createdAt = now - ageHours.hours - (i % 7).minutes
        val text = texts[i % texts.size]
        val author = authors[i % authors.size]
        val photoId = photoIds[i % photoIds.size]
        val recipeId = recipeIds[i % recipeIds.size]
        val initialVote = when (i % 5) {
            1 -> VoteDirectionDto.UP
            4 -> VoteDirectionDto.DOWN
            else -> VoteDirectionDto.NONE
        }
        PostResponseDto(
            id = "post_$i",
            author = author,
            text = text,
            images = listOfNotNull(photoId),
            recipe = recipeId?.let {
                PostRecipeRefDto(id = it, status = PostRecipeStatusDto.AVAILABLE)
            },
            rating = (i * 7 % 200) - 30,
            userVote = initialVote,
            commentsCount = (i * 3) % 50,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }
}

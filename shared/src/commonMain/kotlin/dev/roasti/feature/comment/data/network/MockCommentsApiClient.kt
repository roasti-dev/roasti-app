package dev.roasti.feature.comment.data.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import dev.roasti.feature.comment.data.remote.model.request.CreateCommentRequestDto
import dev.roasti.feature.comment.data.remote.model.request.UpdateCommentRequestDto
import dev.roasti.feature.comment.data.remote.model.response.CommentAuthorDto
import dev.roasti.feature.comment.data.remote.model.response.CommentResponseDto
import dev.roasti.feature.comment.data.remote.model.response.CommentThreadResponseDto
import dev.roasti.core.network.PageResponseDto
import dev.roasti.core.network.PaginationResponseDto
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private const val SimulatedLatencyMillis = 350L

class MockCommentsApiClient : CommentsApiClient {

    private val mutex = Mutex()
    private val storage: MutableMap<String, MutableList<CommentThreadResponseDto>> = mutableMapOf()

    override suspend fun listComments(
        postId: String,
        page: Int,
        limit: Int,
    ): Result<PageResponseDto<CommentThreadResponseDto>> {
        delay(SimulatedLatencyMillis)
        return mutex.withLock {
            val threads = storage.getOrPut(postId) { generateThreads(postId).toMutableList() }
            val total = threads.size
            val lastPage = max(1, ceil(total.toDouble() / limit).toInt())
            val safePage = page.coerceAtLeast(1)
            val from = (safePage - 1) * limit
            val to = min(from + limit, total)
            val items = if (from in 0..<total) threads.subList(from, to).toList() else emptyList()
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

    override suspend fun createComment(
        postId: String,
        request: CreateCommentRequestDto,
    ): Result<CommentResponseDto> {
        delay(SimulatedLatencyMillis)
        return mutex.withLock {
            val now = Clock.System.now()
            val commentId = "c_${postId}_local_${now.toEpochMilliseconds()}"
            val author = CurrentUserAuthor
            val newComment = CommentResponseDto(
                id = commentId,
                isDeleted = false,
                author = author,
                text = request.text,
                createdAt = now,
                updatedAt = now,
                parentId = request.parentId,
            )
            val threads = storage.getOrPut(postId) { generateThreads(postId).toMutableList() }
            if (request.parentId == null) {
                val newThread = CommentThreadResponseDto(
                    id = newComment.id,
                    isDeleted = false,
                    author = author,
                    text = newComment.text,
                    createdAt = newComment.createdAt,
                    updatedAt = newComment.updatedAt,
                    parentId = null,
                    replies = emptyList(),
                )
                threads.add(0, newThread)
            } else {
                val parentIndex = threads.indexOfFirst { it.id == request.parentId }
                if (parentIndex >= 0) {
                    val parent = threads[parentIndex]
                    threads[parentIndex] = parent.copy(replies = parent.replies + newComment)
                }
            }
            Result.success(newComment)
        }
    }

    override suspend fun updateComment(
        commentId: String,
        request: UpdateCommentRequestDto,
    ): Result<CommentResponseDto> {
        delay(SimulatedLatencyMillis)
        return mutex.withLock {
            for ((postId, threads) in storage) {
                val rootIndex = threads.indexOfFirst { it.id == commentId }
                if (rootIndex >= 0) {
                    val updated = threads[rootIndex].copy(
                        text = request.text,
                        updatedAt = Clock.System.now(),
                    )
                    threads[rootIndex] = updated
                    return@withLock Result.success(
                        CommentResponseDto(
                            id = updated.id,
                            isDeleted = updated.isDeleted,
                            author = updated.author,
                            text = updated.text,
                            createdAt = updated.createdAt,
                            updatedAt = updated.updatedAt,
                            parentId = updated.parentId,
                        )
                    )
                }
                for (i in threads.indices) {
                    val thread = threads[i]
                    val replyIndex = thread.replies.indexOfFirst { it.id == commentId }
                    if (replyIndex >= 0) {
                        val replies = thread.replies.toMutableList()
                        val updatedReply = replies[replyIndex].copy(
                            text = request.text,
                            updatedAt = Clock.System.now(),
                        )
                        replies[replyIndex] = updatedReply
                        threads[i] = thread.copy(replies = replies)
                        return@withLock Result.success(updatedReply)
                    }
                }
                postId // unused
            }
            Result.failure(NoSuchElementException("Comment $commentId not found"))
        }
    }

    override suspend fun deleteComment(commentId: String): Result<Unit> {
        delay(SimulatedLatencyMillis)
        return mutex.withLock {
            for ((_, threads) in storage) {
                val rootIndex = threads.indexOfFirst { it.id == commentId }
                if (rootIndex >= 0) {
                    val current = threads[rootIndex]
                    threads[rootIndex] = current.copy(
                        isDeleted = true,
                        text = "",
                        author = null,
                        updatedAt = Clock.System.now(),
                    )
                    return@withLock Result.success(Unit)
                }
                for (i in threads.indices) {
                    val thread = threads[i]
                    val replyIndex = thread.replies.indexOfFirst { it.id == commentId }
                    if (replyIndex >= 0) {
                        val replies = thread.replies.toMutableList()
                        replies[replyIndex] = replies[replyIndex].copy(
                            isDeleted = true,
                            text = "",
                            author = null,
                            updatedAt = Clock.System.now(),
                        )
                        threads[i] = thread.copy(replies = replies)
                        return@withLock Result.success(Unit)
                    }
                }
            }
            Result.failure(NoSuchElementException("Comment $commentId not found"))
        }
    }
}

private val MockAuthors = listOf(
    CommentAuthorDto("u_a1", "u/sarah_j", "https://picsum.photos/seed/avatar_1/96/96"),
    CommentAuthorDto("u_a2", "u/marcus_brews", "https://picsum.photos/seed/avatar_2/96/96"),
    CommentAuthorDto("u_a3", "u/coffee_lab", "https://picsum.photos/seed/avatar_3/96/96"),
    CommentAuthorDto("u_a4", "u/dark_roast", "https://picsum.photos/seed/avatar_4/96/96"),
    CommentAuthorDto("u_a5", "u/single_origin", "https://picsum.photos/seed/avatar_5/96/96"),
    CommentAuthorDto("u_a6", "u/bean_counter", "https://picsum.photos/seed/avatar_6/96/96"),
    CommentAuthorDto("u_a7", "u/grindset", "https://picsum.photos/seed/avatar_7/96/96"),
)

private val CurrentUserAuthor = CommentAuthorDto(
    id = "u_current",
    username = "u/you",
    avatarId = "https://picsum.photos/seed/avatar_current/96/96",
)

private val MockBodies = listOf(
    "Tried this last weekend and it completely changed my morning routine. The body and crema are unreal.",
    "What grind size are you running here? I keep getting channeling on similar beans.",
    "I'd dial the temperature down another degree, but otherwise this is solid advice.",
    "Beautiful shot. The roast date matters way more than people realize.",
    "Honestly the V60 is overrated for this profile. Try a Kalita Wave instead.",
    "Been doing this for years, glad to see it catching on.",
    "Where did you source the beans? Looking for a similar profile.",
    "This is the way. The coarser grind made all the difference for me.",
    "Mine never came out this clean. Probably my water — I'm on hard tap.",
    "Can confirm — flash chilling preserves so much more aroma than ice melt.",
    "Tasted like raisin and cocoa, exactly as advertised. Very impressed.",
    "Disagree on the temperature, but each to their own.",
)

/**
 * Deterministic generator: same postId → same thread set on every call (until the mock
 * is mutated via createComment). Comment counts vary 0..30 depending on a hash of postId,
 * with one bucket producing the empty state.
 */
private fun generateThreads(postId: String): List<CommentThreadResponseDto> {
    val seed = postId.hashCode()
    val rng = SimpleRng(seed)

    val emptyBucket = (seed % 7 == 0)
    val total = if (emptyBucket) 0 else 8 + (seed.mod(23))

    val now: Instant = Clock.System.now()

    return List(total) { i ->
        val author = MockAuthors[(seed + i).mod(MockAuthors.size)]
        val createdAt = now - (i * 2 + 1).hours - (i % 13).minutes
        val isDeleted = (rng.next().mod(11)) == 0
        val text = MockBodies[(seed + i * 3).mod(MockBodies.size)]
        val replyCount = when {
            isDeleted -> rng.next().mod(2) // удалённый может всё ещё иметь replies
            else -> rng.next().mod(4)
        }
        val replies = List(replyCount) { r ->
            val replyAuthor = MockAuthors[(seed + i + r * 5).mod(MockAuthors.size)]
            val replyDeleted = rng.next().mod(15) == 0
            CommentResponseDto(
                id = "c_${postId}_${i}_$r",
                isDeleted = replyDeleted,
                author = if (replyDeleted) null else replyAuthor,
                text = if (replyDeleted) "" else MockBodies[(seed + i + r * 7).mod(MockBodies.size)],
                createdAt = createdAt + (r + 1).minutes * 7,
                updatedAt = createdAt + (r + 1).minutes * 7,
                parentId = "c_${postId}_$i",
            )
        }
        CommentThreadResponseDto(
            id = "c_${postId}_$i",
            isDeleted = isDeleted,
            author = if (isDeleted) null else author,
            text = if (isDeleted) "" else text,
            createdAt = createdAt,
            updatedAt = createdAt,
            parentId = null,
            replies = replies,
        )
    }
}

private class SimpleRng(seed: Int) {
    private var state: Int = if (seed == 0) 1 else seed
    fun next(): Int {
        state = state xor (state shl 13)
        state = state xor (state ushr 17)
        state = state xor (state shl 5)
        return state and Int.MAX_VALUE
    }
}

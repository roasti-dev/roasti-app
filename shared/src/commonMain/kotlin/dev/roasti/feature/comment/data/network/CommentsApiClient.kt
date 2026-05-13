package dev.roasti.feature.comment.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import dev.roasti.core.network.ApiRoutes
import dev.roasti.core.network.AuthorizedRequestExecutor
import dev.roasti.feature.comment.data.remote.model.request.CreateCommentRequestDto
import dev.roasti.feature.comment.data.remote.model.request.UpdateCommentRequestDto
import dev.roasti.feature.comment.data.remote.model.response.CommentResponseDto
import dev.roasti.core.network.PageResponseDto
import dev.roasti.feature.comment.data.remote.model.response.CommentThreadResponseDto

interface CommentsApiClient {
    suspend fun listComments(
        postId: String,
        page: Int,
        limit: Int,
    ): Result<PageResponseDto<CommentThreadResponseDto>>

    suspend fun createComment(
        postId: String,
        request: CreateCommentRequestDto,
    ): Result<CommentResponseDto>

    suspend fun updateComment(
        commentId: String,
        request: UpdateCommentRequestDto,
    ): Result<CommentResponseDto>

    suspend fun deleteComment(commentId: String): Result<Unit>
}

class CommentsApiClientImpl(
    private val httpClient: HttpClient,
    private val authorizedRequestExecutor: AuthorizedRequestExecutor,
) : CommentsApiClient {

    override suspend fun listComments(
        postId: String,
        page: Int,
        limit: Int,
    ): Result<PageResponseDto<CommentThreadResponseDto>> = authorizedRequestExecutor.execute {
        httpClient.get(ApiRoutes.postComments(postId)) {
            url {
                parameters.append("page", page.toString())
                parameters.append("limit", limit.toString())
            }
        }.body<PageResponseDto<CommentThreadResponseDto>>()
    }

    override suspend fun createComment(
        postId: String,
        request: CreateCommentRequestDto,
    ): Result<CommentResponseDto> = authorizedRequestExecutor.execute {
        httpClient.post(ApiRoutes.postComments(postId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<CommentResponseDto>()
    }

    override suspend fun updateComment(
        commentId: String,
        request: UpdateCommentRequestDto,
    ): Result<CommentResponseDto> = authorizedRequestExecutor.execute {
        httpClient.patch(ApiRoutes.commentById(commentId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<CommentResponseDto>()
    }

    override suspend fun deleteComment(commentId: String): Result<Unit> =
        authorizedRequestExecutor.execute {
            httpClient.delete(ApiRoutes.commentById(commentId))
            Unit
        }
}

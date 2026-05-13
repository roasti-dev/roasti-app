package dev.roasti.feature.post.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import dev.roasti.core.network.ApiRoutes
import dev.roasti.core.network.AuthorizedRequestExecutor
import dev.roasti.feature.post.data.remote.model.request.CreatePostRequestDto
import dev.roasti.feature.post.data.remote.model.request.UpdatePostRequestDto
import dev.roasti.feature.post.data.remote.model.request.VoteRequestDto
import dev.roasti.core.network.PageResponseDto
import dev.roasti.feature.post.data.remote.model.response.PostResponseDto
import dev.roasti.feature.post.data.remote.model.response.PostVoteResponseDto

interface PostsApiClient {
    suspend fun getPosts(
        page: Int,
        limit: Int,
    ): Result<PageResponseDto<PostResponseDto>>

    suspend fun getPost(id: String): Result<PostResponseDto>

    suspend fun vote(id: String, request: VoteRequestDto): Result<PostVoteResponseDto>

    suspend fun createPost(request: CreatePostRequestDto): Result<PostResponseDto>

    suspend fun updatePost(id: String, request: UpdatePostRequestDto): Result<PostResponseDto>

    suspend fun deletePost(id: String): Result<Unit>
}

class PostsApiClientImpl(
    private val httpClient: HttpClient,
    private val authorizedRequestExecutor: AuthorizedRequestExecutor,
) : PostsApiClient {

    override suspend fun getPosts(
        page: Int,
        limit: Int,
    ): Result<PageResponseDto<PostResponseDto>> = authorizedRequestExecutor.execute {
        httpClient.get(ApiRoutes.Posts) {
            url {
                parameters.append("page", page.toString())
                parameters.append("limit", limit.toString())
            }
        }.body<PageResponseDto<PostResponseDto>>()
    }

    override suspend fun getPost(id: String): Result<PostResponseDto> =
        authorizedRequestExecutor.execute {
            httpClient.get(ApiRoutes.postById(id)).body<PostResponseDto>()
        }

    override suspend fun vote(
        id: String,
        request: VoteRequestDto,
    ): Result<PostVoteResponseDto> = authorizedRequestExecutor.execute {
        httpClient.put(ApiRoutes.postVote(id)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<PostVoteResponseDto>()
    }

    override suspend fun createPost(
        request: CreatePostRequestDto,
    ): Result<PostResponseDto> = authorizedRequestExecutor.execute {
        httpClient.post(ApiRoutes.Posts) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<PostResponseDto>()
    }

    override suspend fun updatePost(
        id: String,
        request: UpdatePostRequestDto,
    ): Result<PostResponseDto> = authorizedRequestExecutor.execute {
        httpClient.put(ApiRoutes.postById(id)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<PostResponseDto>()
    }

    override suspend fun deletePost(id: String): Result<Unit> =
        authorizedRequestExecutor.execute {
            httpClient.delete(ApiRoutes.postById(id))
            Unit
        }
}

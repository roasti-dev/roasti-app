package dev.roasti.core.config

object AppConfig {
    const val BASE_HOST = "api.roasti.ru"
    const val BASE_URL = "https://$BASE_HOST"

    /**
     * When true, the Posts feed runs against [MockPostsApiClient] instead of hitting the network.
     * Flip to false once the real backend is wired up.
     */
    const val USE_MOCK_POSTS_API = false

    /**
     * When true, comments run against MockCommentsApiClient. Flip to false when the real
     * /api/v1/posts/{post_id}/comments endpoint is live.
     */
    const val USE_MOCK_COMMENTS_API = false
}

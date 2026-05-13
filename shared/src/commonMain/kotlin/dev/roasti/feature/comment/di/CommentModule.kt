package dev.roasti.feature.comment.di

import org.koin.dsl.module
import dev.roasti.core.config.AppConfig
import dev.roasti.feature.comment.data.network.CommentsApiClient
import dev.roasti.feature.comment.data.network.CommentsApiClientImpl
import dev.roasti.feature.comment.data.network.MockCommentsApiClient

val commentModule = module {
    single<CommentsApiClient> {
        if (AppConfig.USE_MOCK_COMMENTS_API) {
            MockCommentsApiClient()
        } else {
            CommentsApiClientImpl(httpClient = get(), authorizedRequestExecutor = get())
        }
    }
}

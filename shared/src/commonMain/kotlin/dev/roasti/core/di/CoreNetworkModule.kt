package dev.roasti.core.di

import org.koin.dsl.module
import dev.roasti.core.network.AuthorizedRequestExecutor
import dev.roasti.core.network.createHttpClient
import dev.roasti.core.session.SessionRepository

val coreNetworkModule = module {
    single {
        val sessionRepository: SessionRepository = get()
        createHttpClient(
            accessTokenProvider = { sessionRepository.currentSession()?.accessToken }
        )
    }
    single { AuthorizedRequestExecutor(get(), get()) }
}

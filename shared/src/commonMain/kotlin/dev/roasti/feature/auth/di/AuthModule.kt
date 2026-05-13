package dev.roasti.feature.auth.di

import org.koin.dsl.module
import dev.roasti.RoastiDatabaseCache
import dev.roasti.UserQueries
import dev.roasti.feature.auth.data.AuthRepositoryImpl
import dev.roasti.feature.auth.data.local.UserCacheDataSource
import dev.roasti.feature.auth.data.network.AuthApiClient
import dev.roasti.feature.auth.data.network.AuthApiClientImpl
import dev.roasti.feature.auth.data.network.ProfileApiClient
import dev.roasti.feature.auth.data.network.ProfileApiClientImpl
import dev.roasti.feature.auth.data.session.SessionStore
import dev.roasti.feature.auth.data.session.TokenRefreshCoordinator
import dev.roasti.feature.auth.domain.repository.AuthRepository
import dev.roasti.core.session.SessionRefresher
import dev.roasti.core.session.SessionRepository

val authModule = module {
    single<UserQueries> { get<RoastiDatabaseCache>().userQueries }
    single { UserCacheDataSource(get()) }
    single<SessionRepository> { SessionStore(get()) }
    single<AuthApiClient> { AuthApiClientImpl(get()) }
    single<ProfileApiClient> { ProfileApiClientImpl(get(), get()) }
    single<SessionRefresher> { TokenRefreshCoordinator(get(), get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get(), get()) }
}

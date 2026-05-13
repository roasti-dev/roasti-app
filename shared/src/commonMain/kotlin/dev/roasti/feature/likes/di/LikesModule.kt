package dev.roasti.feature.likes.di

import org.koin.dsl.bind
import org.koin.dsl.module
import dev.roasti.feature.likes.data.LikesApiClient
import dev.roasti.feature.likes.data.LikesApiClientImpl

val likesModule = module {
    single { LikesApiClientImpl(get(), get()) } bind LikesApiClient::class
}

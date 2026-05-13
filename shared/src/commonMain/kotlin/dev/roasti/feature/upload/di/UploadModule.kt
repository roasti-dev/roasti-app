package dev.roasti.feature.upload.di

import org.koin.dsl.bind
import org.koin.dsl.module
import dev.roasti.feature.upload.data.NetworkUploadRepository
import dev.roasti.feature.upload.data.network.UploadApiClient
import dev.roasti.feature.upload.data.network.UploadApiClientImpl
import dev.roasti.feature.upload.domain.UploadRepository

val uploadModule = module {
    single { UploadApiClientImpl(get(), get()) } bind UploadApiClient::class
    single { NetworkUploadRepository(get()) } bind UploadRepository::class
}

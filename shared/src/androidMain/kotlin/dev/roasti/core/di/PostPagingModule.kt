package dev.roasti.core.di

import org.koin.dsl.module
import dev.roasti.feature.post.data.paging.AllPostsRemoteMediator
import dev.roasti.feature.post.data.paging.PagingPostRepository

val postPagingModule = module {
    single { AllPostsRemoteMediator(get(), get()) }
    single { PagingPostRepository(get(), get(), get()) }
}

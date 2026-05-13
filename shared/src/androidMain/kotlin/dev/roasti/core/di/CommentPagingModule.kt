package dev.roasti.core.di

import org.koin.dsl.module
import dev.roasti.feature.comment.data.paging.PagingCommentRepository

val commentPagingModule = module {
    single { PagingCommentRepository(get(), get()) }
}

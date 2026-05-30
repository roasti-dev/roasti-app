package dev.roasti.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import dev.roasti.core.di.coreDatabaseModule
import dev.roasti.core.di.coreNetworkModule
import dev.roasti.feature.auth.di.authModule
import dev.roasti.feature.comment.di.commentModule
import dev.roasti.feature.likes.di.likesModule
import dev.roasti.feature.post.di.postModule
import dev.roasti.feature.recipe.di.recipeModule
import dev.roasti.feature.upload.di.uploadModule

fun doInitKoin(): KoinApplication = startKoin {
    modules(
        iosPlatformModule,
        coreDatabaseModule,
        coreNetworkModule,
        authModule,
        uploadModule,
        likesModule,
        recipeModule,
        postModule,
        commentModule,
    )
}

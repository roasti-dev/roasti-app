package dev.roasti

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import dev.roasti.core.di.commentPagingModule
import dev.roasti.core.di.coreDatabaseModule
import dev.roasti.core.di.coreNetworkModule
import dev.roasti.core.di.postPagingModule
import dev.roasti.core.di.recipePagingModule
import dev.roasti.di.platformModule
import dev.roasti.di.viewModelsModule
import dev.roasti.feature.auth.di.authModule
import dev.roasti.feature.comment.di.commentModule
import dev.roasti.feature.likes.di.likesModule
import dev.roasti.feature.post.di.postModule
import dev.roasti.feature.recipe.di.recipeModule
import dev.roasti.feature.upload.di.uploadModule

@OptIn(ExperimentalCoilApi::class)
class RoastiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@RoastiApplication)
            modules(
                platformModule,
                coreDatabaseModule,
                coreNetworkModule,
                recipePagingModule,
                postPagingModule,
                commentPagingModule,
                authModule,
                uploadModule,
                likesModule,
                recipeModule,
                postModule,
                commentModule,
                viewModelsModule
            )
        }

        initCoilHttpClient()
    }

    private fun initCoilHttpClient() {
        val httpClient: HttpClient = get()
        SingletonImageLoader.setSafe {
            ImageLoader.Builder(this)
                .components {
                    add(KtorNetworkFetcherFactory(httpClient)) // твой HttpClient из shared
                }
                .build()
        }
    }
}

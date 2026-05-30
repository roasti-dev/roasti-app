package dev.roasti.di

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import dev.roasti.feature.auth.domain.repository.AuthRepository

class IosDependencies : KoinComponent {
    val authRepository: AuthRepository by inject()
}

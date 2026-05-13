package dev.roasti.ui.features.auth

private const val FallbackAuthErrorMessage = "Something went wrong. Please try again."

//internal fun Throwable.toAuthUiMessage(): String = message?.takeIf { it.isNotBlank() } ?: FallbackAuthErrorMessage
internal fun Throwable.toAuthUiMessage(): String = FallbackAuthErrorMessage
package dev.roasti.ui.uikit.state

import androidx.annotation.StringRes
import dev.roasti.R

data class UiError(
    @StringRes val messageRes: Int,
    val isRetriable: Boolean = true,
) {
    companion object {
        val Generic = UiError(R.string.error_generic)
    }
}

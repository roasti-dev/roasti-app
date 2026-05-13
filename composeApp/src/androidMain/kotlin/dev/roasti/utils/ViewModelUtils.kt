package dev.roasti.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

context(viewModel: ViewModel)
fun <T> Flow<T>.stateInWhileSubscribe(
    initialValue: T,
    stopTimeout: Long = 5_000,
): StateFlow<T> = stateIn(
    viewModel.viewModelScope,
    SharingStarted.WhileSubscribed(stopTimeout),
    initialValue
)


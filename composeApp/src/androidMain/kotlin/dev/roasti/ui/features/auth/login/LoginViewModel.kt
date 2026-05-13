package dev.roasti.ui.features.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dev.roasti.feature.auth.domain.repository.AuthRepository
import dev.roasti.ui.features.auth.toAuthUiMessage

private const val EmptyUsernameMessage = "Enter your username."
private const val EmptyPasswordMessage = "Enter your password."

data class LoginFormState(
    val username: String = "",
    val password: String = "",
)

data class LoginUiState(
    val form: LoginFormState,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
) {

    companion object {
        fun initial() = LoginUiState(LoginFormState())

        fun loading(form: LoginFormState) = LoginUiState(
            form = form,
            isLoading = true,
        )

        fun error(form: LoginFormState, message: String) = LoginUiState(
            form = form,
            isError = true,
            errorMessage = message,
        )

        fun content(form: LoginFormState) = LoginUiState(form)
    }
}

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val mutableUiState =
        MutableStateFlow<LoginUiState>(LoginUiState.initial())

    val uiState: StateFlow<LoginUiState> = mutableUiState.asStateFlow()

    fun updateUsername(username: String) {
        updateForm { copy(username = username) }
    }

    fun updatePassword(password: String) {
        updateForm { copy(password = password) }
    }

    fun login() {
        val form = currentForm()
        val validationMessage = validate(form)
        if (validationMessage != null) {
            mutableUiState.value = LoginUiState.error(form, validationMessage)
        } else {
            mutableUiState.value = LoginUiState.loading(form)
            viewModelScope.launch {
                authRepository.login(
                    username = form.username.trim(),
                    password = form.password,
                ).onFailure {
                    mutableUiState.value = LoginUiState.error(form, it.toAuthUiMessage())
                }
            }
        }


    }

    private fun updateForm(transform: LoginFormState.() -> LoginFormState) {
        mutableUiState.value = LoginUiState.content(currentForm().transform())
    }

    private fun currentForm(): LoginFormState = mutableUiState.value.form

    private fun validate(form: LoginFormState): String? = when {
        form.username.isBlank() -> EmptyUsernameMessage
        form.password.isBlank() -> EmptyPasswordMessage
        else -> null
    }
}

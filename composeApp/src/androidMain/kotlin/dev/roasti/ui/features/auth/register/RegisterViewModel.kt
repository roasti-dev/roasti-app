package dev.roasti.ui.features.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dev.roasti.feature.auth.domain.repository.AuthRepository
import dev.roasti.ui.features.auth.toAuthUiMessage

private const val EmptyUsernameMessage = "Choose a username."
private const val EmptyEmailMessage = "Enter your email."
private const val InvalidEmailMessage = "Enter a valid email."
private const val EmptyPasswordMessage = "Create a password."

data class RegisterFormState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val bio: String = "",
)

data class RegisterUiState(
    val form: RegisterFormState,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
) {
    companion object {
        fun initial() = RegisterUiState(RegisterFormState())

        fun loading(form: RegisterFormState) = RegisterUiState(
            form = form,
            isLoading = true,
        )

        fun error(form: RegisterFormState, message: String) = RegisterUiState(
            form = form,
            isError = true,
            errorMessage = message
        )

        fun content(form: RegisterFormState) = RegisterUiState(form)
    }
}

class RegisterViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow<RegisterUiState>(RegisterUiState.initial())

    val uiState: StateFlow<RegisterUiState> = mutableUiState.asStateFlow()

    fun updateUsername(username: String) {
        updateForm { copy(username = username) }
    }

    fun updateEmail(email: String) {
        updateForm { copy(email = email) }
    }

    fun updatePassword(password: String) {
        updateForm { copy(password = password) }
    }

    fun updateBio(bio: String) {
        updateForm { copy(bio = bio) }
    }

    fun register() {
        val form = currentForm()
        val validationMessage = validate(form)
        if (validationMessage != null) {
            mutableUiState.value = RegisterUiState.error(form, validationMessage)
            return
        }

        mutableUiState.value = RegisterUiState.loading(form)
        viewModelScope.launch {
            authRepository.register(
                username = form.username.trim(),
                email = form.email.trim(),
                password = form.password,
                bio = form.bio.trim().ifBlank { null },
                avatarId = null,
            ).onFailure {
                mutableUiState.value = RegisterUiState.error(form, it.toAuthUiMessage())
            }
        }
    }

    private fun updateForm(transform: RegisterFormState.() -> RegisterFormState) {
        mutableUiState.value = RegisterUiState.content(currentForm().transform())
    }

    private fun currentForm(): RegisterFormState = mutableUiState.value.form

    private fun validate(form: RegisterFormState): String? = when {
        form.username.isBlank() -> EmptyUsernameMessage
        form.email.isBlank() -> EmptyEmailMessage
        !form.email.contains("@") -> InvalidEmailMessage
        form.password.isBlank() -> EmptyPasswordMessage
        else -> null
    }
}

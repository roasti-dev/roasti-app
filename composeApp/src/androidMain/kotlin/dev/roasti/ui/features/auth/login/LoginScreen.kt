package dev.roasti.ui.features.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import dev.roasti.ui.features.auth.components.AuthInputField
import dev.roasti.ui.features.auth.components.AuthScreenFrame
import dev.roasti.ui.theme.RoastiTheme

private const val Title = "Welcome back"
private const val Subtitle = "Pick up where you left off and keep your recipes, profile and sessions in sync."
private const val PrimaryActionLabel = "Sign in"
private const val FooterPrompt = "Need an account?"
private const val FooterActionLabel = "Create one"
private const val UsernameLabel = "Username"
private const val UsernamePlaceholder = "coffee_nomad"
private const val PasswordLabel = "Password"
private const val PasswordPlaceholder = "Your password"

@Composable
internal fun LoginRoute(
    onNavigateToRegister: () -> Unit,
) {
    val viewModel: LoginViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(contentWindowInsets = WindowInsets(0)) { innerPaddings ->
        Box(
            Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.background,
                        )
                    )
                )
                .padding(innerPaddings)
        ) {
            LoginScreenContent(
                state = state,
                isSubmitting = state.isLoading,
                errorMessage = state.errorMessage,
                onUsernameChanged = viewModel::updateUsername,
                onPasswordChanged = viewModel::updatePassword,
                onSubmit = viewModel::login,
                onNavigateToRegister = onNavigateToRegister,
            )
        }
    }
}

@Composable
private fun LoginScreenContent(
    state: LoginUiState,
    isSubmitting: Boolean,
    errorMessage: String?,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    AuthScreenFrame(
        title = Title,
        subtitle = Subtitle,
        primaryActionLabel = PrimaryActionLabel,
        onPrimaryAction = onSubmit,
        footerPrompt = FooterPrompt,
        footerActionLabel = FooterActionLabel,
        onFooterAction = onNavigateToRegister,
        isSubmitting = isSubmitting,
        errorMessage = errorMessage,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .systemBarsPadding(),
    ) {
        AuthInputField(
            value = state.form.username,
            onValueChange = onUsernameChanged,
            label = UsernameLabel,
            placeholder = UsernamePlaceholder,
        )
        AuthInputField(
            value = state.form.password,
            onValueChange = onPasswordChanged,
            label = PasswordLabel,
            placeholder = PasswordPlaceholder,
            visualTransformation = PasswordVisualTransformation(),
        )
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    RoastiTheme {
        LoginScreenContent(
            state = LoginUiState(
                form = LoginFormState(
                    username = "coffee_nomad",
                    password = "hunter2",
                )
            ),
            isSubmitting = false,
            errorMessage = null,
            onUsernameChanged = {},
            onPasswordChanged = {},
            onSubmit = {},
            onNavigateToRegister = {},
        )
    }
}

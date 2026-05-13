package dev.roasti.ui.features.auth.register

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

private const val Title = "Brew your account"
private const val Subtitle =
    "Create a profile once and keep every private flow behind a single, reliable session."
private const val PrimaryActionLabel = "Create account"
private const val FooterPrompt = "Already have an account?"
private const val FooterActionLabel = "Sign in"
private const val UsernameLabel = "Username"
private const val UsernamePlaceholder = "origin_story"
private const val EmailLabel = "Email"
private const val EmailPlaceholder = "name@example.com"
private const val PasswordLabel = "Password"
private const val PasswordPlaceholder = "At least one strong secret"
private const val BioLabel = "Bio"
private const val BioPlaceholder = "Optional. Tell others how you brew."

@Composable
internal fun RegisterRoute(
    onNavigateToLogin: () -> Unit,
) {
    val viewModel: RegisterViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(contentWindowInsets = WindowInsets(0)) { innerPaddings ->
        Box(Modifier
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

            RegisterScreenContent(
                state = state,
                isSubmitting = state.isLoading,
                errorMessage = state.errorMessage,
                onUsernameChanged = viewModel::updateUsername,
                onEmailChanged = viewModel::updateEmail,
                onPasswordChanged = viewModel::updatePassword,
                onBioChanged = viewModel::updateBio,
                onSubmit = viewModel::register,
                onNavigateToLogin = onNavigateToLogin,
            )
        }

    }

}

@Composable
private fun RegisterScreenContent(
    state: RegisterUiState,
    isSubmitting: Boolean,
    errorMessage: String?,
    onUsernameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onBioChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    AuthScreenFrame(
        title = Title,
        subtitle = Subtitle,
        primaryActionLabel = PrimaryActionLabel,
        onPrimaryAction = onSubmit,
        footerPrompt = FooterPrompt,
        footerActionLabel = FooterActionLabel,
        onFooterAction = onNavigateToLogin,
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
            value = state.form.email,
            onValueChange = onEmailChanged,
            label = EmailLabel,
            placeholder = EmailPlaceholder,
        )
        AuthInputField(
            value = state.form.password,
            onValueChange = onPasswordChanged,
            label = PasswordLabel,
            placeholder = PasswordPlaceholder,
            visualTransformation = PasswordVisualTransformation(),
        )
        AuthInputField(
            value = state.form.bio,
            onValueChange = onBioChanged,
            label = BioLabel,
            placeholder = BioPlaceholder,
            singleLine = false,
        )
    }
}

@Preview
@Composable
private fun RegisterScreenPreview() {
    RoastiTheme {
        RegisterScreenContent(
            state = RegisterUiState(
                form = RegisterFormState(
                    username = "origin_story",
                    email = "name@example.com",
                    password = "hunter2",
                    bio = "I chase balanced espresso and sweet filter recipes.",
                )
            ),
            isSubmitting = false,
            errorMessage = null,
            onUsernameChanged = {},
            onEmailChanged = {},
            onPasswordChanged = {},
            onBioChanged = {},
            onSubmit = {},
            onNavigateToLogin = {},
        )
    }
}

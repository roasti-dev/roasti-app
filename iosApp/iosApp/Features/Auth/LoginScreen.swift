import SwiftUI
import Shared

struct LoginScreen: View {
    @State private var model: LoginModel = LoginModel(repository: IosDependencies().authRepository)

    var body: some View {
        ZStack {
            Color.background.ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: RoastiSpacing.xl) {
                    header
                    fieldsSection
                    ErrorBanner(message: model.errorMessage)
                    PrimaryPillButton(
                        title: "Continue",
                        isLoading: model.isSubmitting
                    ) {
                        Task { await model.submit() }
                    }
                    footer
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, RoastiSpacing.lg)
                .padding(.top, RoastiSpacing.xxxxl)
                .padding(.bottom, RoastiSpacing.xxl)
            }
            .scrollDismissesKeyboard(.interactively)
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: RoastiSpacing.sm) {
            Text("Welcome back")
                .font(RoastiTypography.largeTitle)
                .foregroundStyle(Color.foreground)

            Text("Sign in to continue brewing.")
                .font(RoastiTypography.bodyLarge)
                .foregroundStyle(Color.mutedFg)
        }
        .padding(.bottom, RoastiSpacing.lg)
    }

    private var fieldsSection: some View {
        FormSection {
            FormRow(
                placeholder: "Username",
                text: $model.username,
                kind: .text
            )
            FormRow(
                placeholder: "Password",
                text: $model.password,
                kind: .secure,
                showSeparator: false
            )
        }
    }

    private var footer: some View {
        HStack(spacing: RoastiSpacing.xs) {
            Text("Don't have an account?")
                .font(RoastiTypography.bodyMedium)
                .foregroundStyle(Color.mutedFg)
            NavigationLink(value: AuthRoute.register) {
                Text("Sign up")
                    .font(RoastiTypography.bodyMedium.weight(.semibold))
                    .foregroundStyle(Color.brandOrange)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.top, RoastiSpacing.md)
    }
}

#Preview("Light") {
    NavigationStack { LoginScreen() }
}

#Preview("Dark") {
    NavigationStack { LoginScreen() }.preferredColorScheme(.dark)
}

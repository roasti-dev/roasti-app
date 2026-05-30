import SwiftUI
import Shared

struct RegisterScreen: View {
    @State private var model: RegisterModel = RegisterModel(repository: IosDependencies().authRepository)
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack {
            Color.background.ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: RoastiSpacing.xl) {
                    header
                    accountSection
                    profileSection
                    ErrorBanner(message: model.errorMessage)
                    PrimaryPillButton(
                        title: "Create account",
                        isLoading: model.isSubmitting
                    ) {
                        Task { await model.submit() }
                    }
                    footer
                    legalNote
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, RoastiSpacing.lg)
                .padding(.top, RoastiSpacing.xxxl)
                .padding(.bottom, RoastiSpacing.xxl)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .navigationBarBackButtonHidden()
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button {
                    dismiss()
                } label: {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(Color.foreground)
                }
            }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: RoastiSpacing.sm) {
            Text("Create your account")
                .font(RoastiTypography.largeTitle)
                .foregroundStyle(Color.foreground)

            Text("One profile for every brew, recipe and session.")
                .font(RoastiTypography.bodyLarge)
                .foregroundStyle(Color.mutedFg)
        }
        .padding(.bottom, RoastiSpacing.lg)
    }

    private var accountSection: some View {
        VStack(alignment: .leading, spacing: RoastiSpacing.sm) {
            sectionHeader("Account")
            FormSection {
                FormRow(
                    placeholder: "Username",
                    text: $model.username,
                    kind: .text
                )
                FormRow(
                    placeholder: "Email",
                    text: $model.email,
                    kind: .email
                )
                FormRow(
                    placeholder: "Password",
                    text: $model.password,
                    kind: .secure,
                    showSeparator: false
                )
            }
        }
    }

    private var profileSection: some View {
        VStack(alignment: .leading, spacing: RoastiSpacing.sm) {
            sectionHeader("Profile", trailing: "Optional")
            FormSection {
                FormRow(
                    placeholder: "Tell others how you brew.",
                    text: $model.bio,
                    kind: .multiline,
                    showSeparator: false
                )
            }
        }
    }

    private func sectionHeader(_ title: String, trailing: String? = nil) -> some View {
        HStack {
            Text(title.uppercased())
                .font(RoastiTypography.labelSmall)
                .tracking(0.5)
                .foregroundStyle(Color.mutedFg)
            Spacer()
            if let trailing {
                Text(trailing.uppercased())
                    .font(RoastiTypography.labelSmall)
                    .tracking(0.5)
                    .foregroundStyle(Color.mutedFg.opacity(0.7))
            }
        }
        .padding(.horizontal, RoastiSpacing.lg)
    }

    private var footer: some View {
        HStack(spacing: RoastiSpacing.xs) {
            Text("Already have an account?")
                .font(RoastiTypography.bodyMedium)
                .foregroundStyle(Color.mutedFg)
            Button {
                dismiss()
            } label: {
                Text("Sign in")
                    .font(RoastiTypography.bodyMedium.weight(.semibold))
                    .foregroundStyle(Color.brandOrange)
            }
            .buttonStyle(.plain)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, RoastiSpacing.md)
    }

    private var legalNote: some View {
        Text("By creating an account you agree to Roasti's Terms and Privacy Policy.")
            .font(RoastiTypography.bodySmall)
            .foregroundStyle(Color.mutedFg.opacity(0.8))
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, RoastiSpacing.lg)
    }
}

#Preview("Light") {
    NavigationStack { RegisterScreen() }
}

#Preview("Dark") {
    NavigationStack { RegisterScreen() }.preferredColorScheme(.dark)
}

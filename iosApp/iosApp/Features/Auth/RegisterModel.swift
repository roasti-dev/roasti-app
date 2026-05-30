import Foundation
import Observation
import Shared

@MainActor
@Observable
final class RegisterModel {
    var username = ""
    var email = ""
    var password = ""
    var bio = ""
    private(set) var isSubmitting = false
    private(set) var errorMessage: String?

    private let repository: AuthRepository

    init(repository: AuthRepository) {
        self.repository = repository
    }

    func submit() async {
        errorMessage = nil
        let trimmedUsername = username.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedBio = bio.trimmingCharacters(in: .whitespacesAndNewlines)

        if trimmedUsername.isEmpty {
            errorMessage = "Choose a username."
            return
        }
        if trimmedEmail.isEmpty {
            errorMessage = "Enter your email."
            return
        }
        if !trimmedEmail.contains("@") {
            errorMessage = "Enter a valid email."
            return
        }
        if password.isEmpty {
            errorMessage = "Create a password."
            return
        }

        isSubmitting = true
        defer { isSubmitting = false }
        do {
            try await IosAuthExtensionsKt.registerOrThrow(
                repository,
                username: trimmedUsername,
                email: trimmedEmail,
                password: password,
                bio: trimmedBio.isEmpty ? nil : trimmedBio,
                avatarId: nil
            )
        } catch {
            errorMessage = mapErrorMessage(error)
        }
    }

    private func mapErrorMessage(_ error: Error) -> String {
        let nsError = error as NSError
        if nsError.domain == NSURLErrorDomain {
            return "Network unavailable. Try again."
        }
        let description = nsError.localizedDescription
        return description.isEmpty ? "Something went wrong. Try again." : description
    }
}

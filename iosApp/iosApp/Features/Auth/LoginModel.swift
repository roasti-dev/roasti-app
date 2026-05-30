import Foundation
import Observation
import Shared

@MainActor
@Observable
final class LoginModel {
    var username = ""
    var password = ""
    private(set) var isSubmitting = false
    private(set) var errorMessage: String?

    private let repository: AuthRepository

    init(repository: AuthRepository) {
        self.repository = repository
    }

    func submit() async {
        errorMessage = nil
        let trimmedUsername = username.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmedUsername.isEmpty {
            errorMessage = "Enter your username."
            return
        }
        if password.isEmpty {
            errorMessage = "Enter your password."
            return
        }

        isSubmitting = true
        defer { isSubmitting = false }
        do {
            try await IosAuthExtensionsKt.loginOrThrow(
                repository,
                username: trimmedUsername,
                password: password
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

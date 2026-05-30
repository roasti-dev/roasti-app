import Foundation
import Observation
import Shared

@MainActor
@Observable
final class AuthSession {
    private(set) var state: any AuthState

    private let repository: AuthRepository
    @ObservationIgnored private var task: Task<Void, Never>?

    init(repository: AuthRepository) {
        self.repository = repository
        self.state = repository.authState.value
        start()
    }

    deinit {
        task?.cancel()
    }

    func bootstrap() async {
        try? await repository.bootstrap()
    }

    private func start() {
        task = Task { [weak self] in
            guard let stream = self?.repository.authState else { return }
            for await value in stream {
                await MainActor.run { [weak self] in
                    self?.state = value
                }
            }
        }
    }
}

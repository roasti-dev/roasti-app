import SwiftUI
import Shared

struct RootView: View {
    @State private var auth: AuthSession = AuthSession(repository: IosDependencies().authRepository)

    var body: some View {
        Group {
            switch onEnum(of: auth.state) {
            case .loading:
                LoadingView()
            case .guest, .error:
                AuthRootView()
            case .authenticated:
                RoastiScreen()
            }
        }
        .task { await auth.bootstrap() }
    }
}

private struct LoadingView: View {
    var body: some View {
        ZStack {
            Color.background.ignoresSafeArea()
            ProgressView()
        }
    }
}

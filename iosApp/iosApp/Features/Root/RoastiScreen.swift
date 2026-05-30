import SwiftUI

struct RoastiScreen: View {
    var body: some View {
        ZStack {
            Color.background.ignoresSafeArea()
            Text("Roasti")
                .font(.largeTitle)
                .foregroundStyle(Color.foreground)
        }
    }
}

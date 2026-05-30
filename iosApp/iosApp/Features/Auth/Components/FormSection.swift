import SwiftUI

struct FormSection<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        VStack(spacing: 0) {
            content
        }
        .background(Color.surfaceCard)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

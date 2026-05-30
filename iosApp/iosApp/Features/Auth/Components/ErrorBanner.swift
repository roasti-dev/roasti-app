import SwiftUI

struct ErrorBanner: View {
    let message: String?

    var body: some View {
        if let message {
            HStack(alignment: .top, spacing: RoastiSpacing.sm) {
                Image(systemName: "exclamationmark.circle.fill")
                    .foregroundStyle(Color.errorFg)
                Text(message)
                    .font(RoastiTypography.bodyMedium)
                    .foregroundStyle(Color.errorFg)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, RoastiSpacing.lg)
            .padding(.vertical, RoastiSpacing.md)
            .background(Color.errorBg)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .transition(.opacity)
        }
    }
}

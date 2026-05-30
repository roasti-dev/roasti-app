import SwiftUI

enum FormRowKind {
    case text
    case email
    case secure
    case multiline
}

struct FormRow: View {
    let placeholder: String
    @Binding var text: String
    var kind: FormRowKind = .text
    var showSeparator: Bool = true

    @State private var isPasswordRevealed = false

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: RoastiSpacing.sm) {
                inputField
                    .font(RoastiTypography.bodyLarge)
                    .foregroundStyle(Color.foreground)
                    .tint(Color.brandOrange)

                if kind == .secure {
                    Button {
                        isPasswordRevealed.toggle()
                    } label: {
                        Image(systemName: isPasswordRevealed ? "eye.slash" : "eye")
                            .foregroundStyle(Color.mutedFg)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, RoastiSpacing.lg)
            .padding(.vertical, RoastiSpacing.md)
            .frame(minHeight: 52, alignment: .center)

            if showSeparator {
                Divider()
                    .background(Color.borderSubtle)
                    .padding(.leading, RoastiSpacing.lg)
            }
        }
    }

    @ViewBuilder
    private var inputField: some View {
        switch kind {
        case .text:
            TextField(placeholder, text: $text)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
        case .email:
            TextField(placeholder, text: $text)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .keyboardType(.emailAddress)
                .textContentType(.emailAddress)
        case .secure:
            Group {
                if isPasswordRevealed {
                    TextField(placeholder, text: $text)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                } else {
                    SecureField(placeholder, text: $text)
                }
            }
        case .multiline:
            TextField(placeholder, text: $text, axis: .vertical)
                .lineLimit(2...4)
        }
    }
}

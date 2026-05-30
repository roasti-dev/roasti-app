import SwiftUI

enum RoastiTypography {
    static let displayLarge = Font.system(size: 24, weight: .medium)
    static let displayMedium = Font.system(size: 30, weight: .medium)
    static let largeTitle = Font.system(size: 34, weight: .bold)

    static let headlineLarge = Font.system(size: 20, weight: .medium)
    static let titleLarge = Font.system(size: 18, weight: .medium)
    static let titleMedium = Font.system(size: 16, weight: .medium)
    static let titleSmall = Font.system(size: 14, weight: .medium)

    static let bodyLarge = Font.system(size: 16, weight: .regular)
    static let bodyMedium = Font.system(size: 14, weight: .regular)
    static let bodySmall = Font.system(size: 12, weight: .regular)

    static let labelLarge = Font.system(size: 16, weight: .regular)
    static let labelMedium = Font.system(size: 14, weight: .regular)
    static let labelSmall = Font.system(size: 12, weight: .regular)

    static let button = Font.system(size: 17, weight: .semibold)
}

extension Text {
    func displayMediumTracking() -> Text {
        self.tracking(-0.025 * 30)
    }
}

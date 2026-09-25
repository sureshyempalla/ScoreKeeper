import SwiftUI

// Matches the wireframes and the Android theme: warm cream surfaces,
// felt-green primary, amber accent.
extension Color {
    static let skGreen = Color(red: 0x1F / 255, green: 0x6F / 255, blue: 0x54 / 255)
    static let skGreenDark = Color(red: 0x16 / 255, green: 0x4F / 255, blue: 0x3C / 255)
    static let skAmber = Color(red: 0xE0 / 255, green: 0xA4 / 255, blue: 0x38 / 255)
    static let skCream = Color(red: 0xFA / 255, green: 0xF7 / 255, blue: 0xF2 / 255)
    static let skBorder = Color(red: 0xE8 / 255, green: 0xE3 / 255, blue: 0xD9 / 255)
    static let skMuted = Color(red: 0x6B / 255, green: 0x66 / 255, blue: 0x60 / 255)
    static let skDanger = Color(red: 0xC0 / 255, green: 0x46 / 255, blue: 0x3C / 255)

    /// Cycled by `SavedPlayer.colorIndex` for avatar backgrounds, matching Android's `AvatarColors`.
    static let skAvatarColors: [Color] = [
        .skGreen,
        .skAmber,
        Color(red: 0x8B / 255, green: 0x7C / 255, blue: 0xD8 / 255),
        .skDanger,
        Color(red: 0x5B / 255, green: 0x8D / 255, blue: 0xEF / 255),
        Color(red: 0xD0 / 255, green: 0x6B / 255, blue: 0xA6 / 255)
    ]
}

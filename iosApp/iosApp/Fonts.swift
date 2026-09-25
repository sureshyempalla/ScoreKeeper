import SwiftUI

/// The wireframes' two-typeface system: Fraunces (serif display, for
/// headings/titles) over Work Sans (sans body, everything else) — mirrors
/// `Type.kt` on Android. Both are bundled as static-weight OFL fonts
/// (`Fonts/*.ttf`, registered via `UIAppFonts` in Info.plist) rather than
/// fetched from Google Fonts at runtime, so the app looks right offline and
/// on first launch.
extension Font {
    static func fraunces(_ size: CGFloat, weight: Weight = .regular) -> Font {
        switch weight {
        case .bold, .heavy, .black:
            return .custom("Fraunces-Bold", size: size)
        case .semibold, .medium:
            return .custom("Fraunces SemiBold", size: size)
        default:
            return .custom("Fraunces-Regular", size: size)
        }
    }

    static func workSans(_ size: CGFloat, weight: Weight = .regular) -> Font {
        switch weight {
        case .bold, .heavy, .black:
            return .custom("WorkSans-Bold", size: size)
        case .semibold:
            return .custom("Work Sans SemiBold", size: size)
        case .medium:
            return .custom("Work Sans Medium", size: size)
        default:
            return .custom("WorkSans-Regular", size: size)
        }
    }
}

import SwiftUI

/// Placeholder destination for bottom-nav tabs whose flows aren't built yet
/// (Events, Stats, Profile). Mirrors Android's `ComingSoonScreen.kt`.
struct ComingSoonView: View {
    let feature: String
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "hourglass")
                .font(.largeTitle)
                .foregroundStyle(Color.skMuted)
            Text("\(feature) is coming soon")
                .font(.title3.bold())
            Text("We're still building this part of Score Keeper.")
                .font(.subheadline)
                .foregroundStyle(Color.skMuted)
                .multilineTextAlignment(.center)
            Button("Back to Home") { dismiss() }
                .padding(.top, 8)
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.skCream)
        .navigationTitle(feature)
        .navigationBarTitleDisplayMode(.inline)
    }
}

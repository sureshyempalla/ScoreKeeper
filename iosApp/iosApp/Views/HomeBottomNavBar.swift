import SwiftUI

/// Which tab is active, per the wireframe's persistent bottom nav (Main/Leaderboard/Profile).
enum HomeTab {
    case home, events, stats, profile
}

/// Mirrors Android's `HomeBottomNavBar.kt`: a 4-tab bar pinned under Home's content.
struct HomeBottomNavBar: View {
    let selected: HomeTab
    let onHome: () -> Void
    let onEvents: () -> Void
    let onStats: () -> Void
    let onProfile: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            NavTab(systemImage: "house.fill", label: "Home", active: selected == .home, onTap: onHome)
            NavTab(systemImage: "trophy.fill", label: "Events", active: selected == .events, onTap: onEvents)
            NavTab(systemImage: "chart.bar.fill", label: "Stats", active: selected == .stats, onTap: onStats)
            NavTab(systemImage: "person.fill", label: "Profile", active: selected == .profile, onTap: onProfile)
        }
        .frame(height: 64)
        .background(Color.white)
    }
}

private struct NavTab: View {
    let systemImage: String
    let label: String
    let active: Bool
    let onTap: () -> Void

    private var color: Color { active ? .skGreen : .skMuted }

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 4) {
                Image(systemName: systemImage)
                    .foregroundStyle(color)
                Text(label)
                    .font(.caption2)
                    .foregroundStyle(color)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }
}

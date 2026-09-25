import SwiftUI
import Shared

/// Home, per the wireframe (Main.dc.html): a sign-in nudge banner (shown
/// whenever the person isn't fully signed in -- guest included), the Card
/// Games / Community Events entry tiles, a "Continue" section for the most
/// recent unfinished game, a Saved Players roster, and a bottom nav bar.
/// Unlike the earlier build, Home no longer sits behind a login wall: local
/// scorekeeping works fully signed-out, matching the design.
struct HomeView: View {
    @ObservedObject var appVM: AppViewModel
    @ObservedObject var authVM: AuthViewModel
    @Binding var path: [Route]
    @Binding var showLogin: Bool

    @State private var showAddPlayerDialog = false
    @State private var newPlayerName = ""
    @State private var sessionPendingDelete: GameSession?

    private var inProgress: GameSession? {
        appVM.sessions.first { !$0.isFinished }
    }

    private var recentCount: Int {
        let weekMillis: Int64 = 7 * 24 * 60 * 60 * 1000
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        return appVM.sessions.filter { now - $0.createdAtMillis <= weekMillis }.count
    }

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Score Keeper")
                            .font(.largeTitle.bold())
                        Text("\(recentCount) games this week")
                            .font(.subheadline)
                            .foregroundStyle(Color.skMuted)
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 24)

                    if authVM.state?.statusId != AuthStatuses.shared.SIGNED_IN {
                        Button {
                            showLogin = true
                        } label: {
                            HStack {
                                Text("Sign in to sync your data across devices")
                                    .font(.subheadline.weight(.medium))
                                    .foregroundStyle(Color.skGreenDark)
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .foregroundStyle(Color.skGreenDark)
                            }
                            .padding(14)
                            .background(Color(red: 0xEA / 255, green: 0xF3 / 255, blue: 0xEF / 255))
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                        .buttonStyle(.plain)
                        .padding(.horizontal, 20)
                    }

                    VStack(spacing: 12) {
                        HomeTile(
                            title: "Card Games",
                            subtitle: "Score Uno, Rummy, Phase 10 & more",
                            footer: "\(appVM.sessions.count) games this week →",
                            emojis: ["🃏", "🔴", "🔟", "⭐"],
                            containerColor: .white,
                            onDark: false,
                            onTap: { path.append(.gamePicker) }
                        )
                        HomeTile(
                            title: "Community Events",
                            subtitle: "Plan multi-sport tournaments & meetups",
                            footer: "Coming soon →",
                            emojis: ["🏐", "🏓", "🎯", "🪔"],
                            containerColor: .skGreen,
                            onDark: true,
                            onTap: { path.append(.comingSoon(feature: "Events")) }
                        )
                    }
                    .padding(.horizontal, 20)

                    if let inProgress {
                        VStack(alignment: .leading, spacing: 10) {
                            SectionLabel("Continue")
                            ContinueCard(
                                session: inProgress,
                                onTap: { path.append(.scoreEntry(sessionId: inProgress.id)) },
                                onLongPress: { sessionPendingDelete = inProgress }
                            )
                        }
                        .padding(.horizontal, 20)
                    }

                    VStack(alignment: .leading, spacing: 10) {
                        SectionLabel("Saved Players")
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 14) {
                                ForEach(appVM.savedPlayers, id: \.id) { player in
                                    SavedPlayerAvatar(player: player)
                                }
                                Button {
                                    showAddPlayerDialog = true
                                } label: {
                                    VStack(spacing: 6) {
                                        Circle()
                                            .fill(Color(red: 0xEA / 255, green: 0xF3 / 255, blue: 0xEF / 255))
                                            .frame(width: 48, height: 48)
                                            .overlay(Image(systemName: "plus").foregroundStyle(Color.skGreen))
                                        Text("New")
                                            .font(.caption)
                                            .foregroundStyle(Color.skGreen)
                                    }
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    .padding(.horizontal, 20)
                }
                .padding(.bottom, 16)
            }

            HomeBottomNavBar(
                selected: .home,
                onHome: {},
                onEvents: { path.append(.comingSoon(feature: "Events")) },
                onStats: { path.append(.comingSoon(feature: "Stats")) },
                onProfile: { path.append(.comingSoon(feature: "Profile")) }
            )
        }
        .background(Color.skCream)
        .navigationBarHidden(true)
        .alert("Add a player", isPresented: $showAddPlayerDialog) {
            TextField("Name", text: $newPlayerName)
            Button("Cancel", role: .cancel) { newPlayerName = "" }
            Button("Add") {
                let trimmed = newPlayerName.trimmingCharacters(in: .whitespaces)
                if !trimmed.isEmpty {
                    appVM.addSavedPlayer(name: trimmed)
                }
                newPlayerName = ""
            }
        }
        .alert(
            "Delete \"\(sessionPendingDelete?.name ?? "")\"?",
            isPresented: .constant(sessionPendingDelete != nil),
            presenting: sessionPendingDelete
        ) { session in
            Button("Delete", role: .destructive) {
                appVM.deleteSession(sessionId: session.id)
                sessionPendingDelete = nil
            }
            Button("Cancel", role: .cancel) { sessionPendingDelete = nil }
        } message: { _ in
            Text("This removes the game and all its rounds. This can't be undone.")
        }
    }
}

private struct SectionLabel: View {
    let text: String
    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text.uppercased())
            .font(.caption.weight(.semibold))
            .foregroundStyle(Color.skMuted)
    }
}

private struct HomeTile: View {
    let title: String
    let subtitle: String
    let footer: String
    let emojis: [String]
    let containerColor: Color
    let onDark: Bool
    let onTap: () -> Void

    private var textColor: Color { onDark ? .white : .primary }
    private var subColor: Color { onDark ? .white.opacity(0.85) : .skMuted }
    private var footerColor: Color { onDark ? Color(red: 0xFD / 255, green: 0xF3 / 255, blue: 0xE1 / 255) : .skGreen }
    private var chipBg: Color { onDark ? .white.opacity(0.15) : Color(red: 0xEA / 255, green: 0xF3 / 255, blue: 0xEF / 255) }

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    HStack(spacing: 6) {
                        ForEach(emojis, id: \.self) { emoji in
                            Text(emoji)
                                .frame(width: 34, height: 34)
                                .background(chipBg)
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                        }
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .foregroundStyle(onDark ? .white : Color.skMuted)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(.title3.bold()).foregroundStyle(textColor)
                    Text(subtitle).font(.caption).foregroundStyle(subColor)
                }
                Text(footer).font(.caption.weight(.semibold)).foregroundStyle(footerColor)
            }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(containerColor)
            .overlay(
                RoundedRectangle(cornerRadius: 20)
                    .stroke(onDark ? Color.clear : Color.skBorder, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
        .buttonStyle(.plain)
    }
}

private struct ContinueCard: View {
    let session: GameSession
    let onTap: () -> Void
    let onLongPress: () -> Void

    var body: some View {
        HStack {
            Text(session.gameType.emoji)
                .frame(width: 38, height: 38)
                .background(Color(red: 0xEA / 255, green: 0xF3 / 255, blue: 0xEF / 255))
                .clipShape(RoundedRectangle(cornerRadius: 11))
            VStack(alignment: .leading, spacing: 2) {
                Text(session.name).font(.subheadline.weight(.semibold))
                Text("Round \((session.rounds.map { $0.roundNumber }.max() ?? 0) + 1) · \(session.players.count) players")
                    .font(.caption)
                    .foregroundStyle(Color.skMuted)
            }
            .padding(.leading, 12)
            Spacer()
        }
        .padding(14)
        .background(Color.white)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.skBorder, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .contentShape(Rectangle())
        .onTapGesture(perform: onTap)
        .onLongPressGesture(perform: onLongPress)
    }
}

private struct SavedPlayerAvatar: View {
    let player: SavedPlayer

    private var color: Color {
        let colors = Color.skAvatarColors
        let index = Int(player.colorIndex) % colors.count
        return colors[index < 0 ? index + colors.count : index]
    }

    var body: some View {
        VStack(spacing: 6) {
            Circle()
                .fill(color)
                .frame(width: 48, height: 48)
                .overlay(
                    Text(player.name.prefix(1).uppercased())
                        .foregroundStyle(.white)
                        .font(.subheadline.weight(.semibold))
                )
            Text(player.name)
                .font(.caption)
                .foregroundStyle(Color.skMuted)
        }
    }
}

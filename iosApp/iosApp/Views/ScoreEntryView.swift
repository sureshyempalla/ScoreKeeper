import SwiftUI
import Shared

private struct RoundEntry {
    var outcomeId: String = "NORMAL"
    var text: String = ""
}

struct ScoreEntryView: View {
    let sessionId: String
    @ObservedObject var appVM: AppViewModel
    @StateObject private var sessionVM: SessionViewModel
    @Binding var path: [Route]

    @State private var entries: [String: RoundEntry] = [:]

    init(sessionId: String, appVM: AppViewModel, path: Binding<[Route]>) {
        self.sessionId = sessionId
        self.appVM = appVM
        self._path = path
        self._sessionVM = StateObject(wrappedValue: SessionViewModel(controller: appVM.controller, sessionId: sessionId))
    }

    var body: some View {
        Group {
            if let session = sessionVM.session {
                content(for: session)
            } else {
                ProgressView()
            }
        }
        .navigationTitle(sessionVM.session?.name ?? "")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    appVM.undoLastRound(sessionId: sessionId)
                } label: {
                    Label("Undo", systemImage: "arrow.uturn.backward")
                }
                .disabled((sessionVM.session?.rounds.count ?? 0) == 0)
            }
        }
    }

    @ViewBuilder
    private func content(for session: GameSession) -> some View {
        let standings = appVM.controller.standings(session: session)
        let isRummy = session.gameType.name == "RUMMY"
        let gameOver = appVM.controller.isGameOver(session: session)
        let nextRound = appVM.controller.nextRoundNumber(session: session)
        let activePlayers = session.players.filter { p in
            !(standings.first { $0.player.id == p.id }?.isEliminated ?? false)
        }

        List {
            Section("Standings") {
                ForEach(standings, id: \.player.id) { standing in
                    HStack {
                        Text("#\(standing.rank)")
                        Text(standing.player.name + (standing.isEliminated ? " (out)" : ""))
                            .foregroundStyle(standing.isEliminated ? Color.skDanger : .primary)
                        Spacer()
                        Text("\(standing.total) pts")
                    }
                }
            }

            if !gameOver {
                Section("Round \(nextRound)") {
                    ForEach(activePlayers, id: \.id) { player in
                        RoundEntryRow(
                            player: player,
                            isRummy: isRummy,
                            entry: binding(for: player.id)
                        )
                    }
                }
            }
        }
        .safeAreaInset(edge: .bottom) {
            Button {
                if gameOver {
                    appVM.finishSession(sessionId: sessionId)
                    path.removeAll()
                    path.append(.summary(sessionId: sessionId))
                } else {
                    submitRound(session: session, nextRound: nextRound)
                }
            } label: {
                Text(gameOver ? "Finish Game" : "Submit Round \(nextRound)")
                    .frame(maxWidth: .infinity)
                    .padding()
            }
            .buttonStyle(.borderedProminent)
            .padding()
        }
    }

    private func binding(for playerId: String) -> Binding<RoundEntry> {
        Binding(
            get: { entries[playerId] ?? RoundEntry() },
            set: { entries[playerId] = $0 }
        )
    }

    private func submitRound(session: GameSession, nextRound: Int32) {
        for player in session.players {
            let entry = entries[player.id] ?? RoundEntry()
            let raw: Int32
            if session.gameType.name == "RUMMY" {
                raw = InteropHelpersKt.rummyPenaltyForOutcomeId(outcomeId: entry.outcomeId, rules: session.rules, enteredDeadwood: Int32(entry.text) ?? 0)
            } else {
                raw = Int32(entry.text) ?? 0
            }
            appVM.recordRound(sessionId: sessionId, playerId: player.id, roundNumber: nextRound, rawScore: raw, outcomeId: entry.outcomeId)
        }
        entries = [:]
    }
}

private struct RoundEntryRow: View {
    let player: Player
    let isRummy: Bool
    @Binding var entry: RoundEntry

    private let outcomes: [(id: String, label: String)] = [
        ("WIN", "Win"), ("FIRST_DROP", "1st Drop"), ("MIDDLE_DROP", "Mid Drop"), ("FULL_COUNT", "Full Count")
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(player.name).font(.subheadline).bold()
            if isRummy {
                HStack(spacing: 6) {
                    ForEach(outcomes, id: \.id) { outcome in
                        Button(outcome.label) {
                            entry.outcomeId = entry.outcomeId == outcome.id ? "NORMAL" : outcome.id
                        }
                        .font(.caption)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(entry.outcomeId == outcome.id ? Color.skGreen : Color.skBorder)
                        .foregroundStyle(entry.outcomeId == outcome.id ? .white : .primary)
                        .clipShape(Capsule())
                    }
                }
                if entry.outcomeId == "NORMAL" || entry.outcomeId == "WIN" {
                    TextField(entry.outcomeId == "WIN" ? "Points (usually 0)" : "Deadwood points", text: $entry.text)
                        .keyboardType(.numberPad)
                        .textFieldStyle(.roundedBorder)
                }
            } else {
                TextField("Points this round", text: $entry.text)
                    .keyboardType(.numberPad)
                    .textFieldStyle(.roundedBorder)
            }
        }
        .padding(.vertical, 4)
    }
}

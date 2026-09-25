import SwiftUI
import Shared

struct AddPlayersView: View {
    let gameTypeId: String
    @ObservedObject var appVM: AppViewModel
    @Binding var path: [Route]

    private var gameType: GameType { GameTypes.shared.byId(id: gameTypeId) }

    @State private var sessionName: String = ""
    @State private var playerNames: [String] = ["", ""]

    // Rummy-only house rules.
    @State private var poolLimit: String = "200"
    @State private var firstDrop: String = "25"
    @State private var middleDrop: String = "40"
    @State private var fullCount: String = "80"

    private var isRummy: Bool { gameTypeId == "RUMMY" }

    private var canStart: Bool {
        playerNames.filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }.count >= 2
    }

    var body: some View {
        Form {
            Section {
                TextField("Game name", text: $sessionName)
            }
            Section("Players") {
                ForEach(playerNames.indices, id: \.self) { index in
                    HStack {
                        TextField("Player \(index + 1)", text: $playerNames[index])
                        if playerNames.count > 2 {
                            Button {
                                playerNames.remove(at: index)
                            } label: {
                                Image(systemName: "minus.circle.fill").foregroundStyle(Color.skDanger)
                            }
                        }
                    }
                }
                Button("+ Add player") { playerNames.append("") }
            }
            if isRummy {
                Section("Rummy pool rules") {
                    ruleField("Pool limit (eliminated above this)", value: $poolLimit)
                    ruleField("First drop penalty", value: $firstDrop)
                    ruleField("Middle drop penalty", value: $middleDrop)
                    ruleField("Full count penalty", value: $fullCount)
                }
            }
        }
        .navigationTitle("\(gameType.emoji) \(gameType.displayName)")
        .onAppear {
            if sessionName.isEmpty { sessionName = "\(gameType.displayName) Game" }
        }
        .safeAreaInset(edge: .bottom) {
            Button(action: startGame) {
                Text("Start Game")
                    .frame(maxWidth: .infinity)
                    .padding()
            }
            .buttonStyle(.borderedProminent)
            .disabled(!canStart)
            .padding()
        }
    }

    private func ruleField(_ label: String, value: Binding<String>) -> some View {
        HStack {
            Text(label)
            Spacer()
            TextField("", text: value)
                .keyboardType(.numberPad)
                .multilineTextAlignment(.trailing)
                .frame(width: 60)
        }
    }

    private func startGame() {
        let names = playerNames.map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
        guard names.count >= 2 else { return }

        let rules: GameRules
        if isRummy {
            rules = InteropHelpersKt.rummyRules(
                poolLimit: Int32(poolLimit) ?? 200,
                firstDropPenalty: Int32(firstDrop) ?? 25,
                middleDropPenalty: Int32(middleDrop) ?? 40,
                fullCountPenalty: Int32(fullCount) ?? 80
            )
        } else {
            rules = InteropHelpersKt.defaultGameRules()
        }

        let name = sessionName.isEmpty ? "\(gameType.displayName) Game" : sessionName
        appVM.newGame(gameTypeId: gameTypeId, sessionName: name, playerNames: names, rules: rules) { sessionId in
            path.removeAll()
            path.append(.scoreEntry(sessionId: sessionId))
        }
    }
}

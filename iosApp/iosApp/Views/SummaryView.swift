import SwiftUI
import Shared

struct SummaryView: View {
    let sessionId: String
    @ObservedObject var appVM: AppViewModel
    @StateObject private var sessionVM: SessionViewModel
    @Binding var path: [Route]

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
    }

    @ViewBuilder
    private func content(for session: GameSession) -> some View {
        let standings = appVM.controller.standings(session: session)
        let winner = standings.min { $0.rank < $1.rank }

        VStack(alignment: .leading, spacing: 8) {
            Text("\(session.gameType.emoji) \(session.name)")
                .font(.title2).bold()
                .padding(.horizontal)
            Text("🏆 \(winner?.player.name ?? "—") wins!")
                .font(.title3)
                .padding(.horizontal)
                .padding(.bottom, 8)

            List {
                ForEach(standings, id: \.player.id) { standing in
                    HStack {
                        Text("#\(standing.rank)").font(.headline)
                        Text(standing.player.name)
                        Spacer()
                        Text("\(standing.total) pts")
                    }
                    .listRowBackground(standing.rank == 1 ? Color.skAmber.opacity(0.25) : nil)
                }
            }
            .listStyle(.plain)

            Button {
                path.removeAll()
            } label: {
                Text("Back to Home")
                    .frame(maxWidth: .infinity)
                    .padding()
            }
            .buttonStyle(.borderedProminent)
            .padding()
        }
        .navigationBarBackButtonHidden(true)
    }
}

import SwiftUI
import Shared

struct HomeView: View {
    @ObservedObject var appVM: AppViewModel
    @Binding var path: [Route]

    var body: some View {
        List {
            if appVM.sessions.isEmpty {
                VStack(spacing: 6) {
                    Text("No games yet")
                        .font(.headline)
                    Text("Tap \"New Game\" to pick a game and start keeping score")
                        .font(.subheadline)
                        .foregroundStyle(Color.skMuted)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 40)
                .listRowSeparator(.hidden)
            } else {
                ForEach(appVM.sessions, id: \.id) { session in
                    Button {
                        path.append(.scoreEntry(sessionId: session.id))
                    } label: {
                        SessionRow(session: session)
                    }
                    .buttonStyle(.plain)
                    .swipeActions {
                        Button(role: .destructive) {
                            appVM.deleteSession(sessionId: session.id)
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    }
                }
            }
        }
        .listStyle(.plain)
        .navigationTitle("Score Keeper")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    path.append(.gamePicker)
                } label: {
                    Label("New Game", systemImage: "plus")
                }
            }
        }
    }
}

private struct SessionRow: View {
    let session: GameSession

    var body: some View {
        HStack {
            Text(session.gameType.emoji)
                .font(.largeTitle)
            VStack(alignment: .leading, spacing: 2) {
                Text(session.name)
                    .font(.headline)
                Text("\(session.gameType.displayName) · \(session.players.count) players" + (session.isFinished ? " · Finished" : ""))
                    .font(.subheadline)
                    .foregroundStyle(Color.skMuted)
            }
            Spacer()
        }
        .padding(.vertical, 6)
    }
}

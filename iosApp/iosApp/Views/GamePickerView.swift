import SwiftUI
import Shared

struct GamePickerView: View {
    @Binding var path: [Route]

    private let columns = [GridItem(.flexible()), GridItem(.flexible())]

    var body: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(GameTypes.shared.all, id: \.name) { gameType in
                    Button {
                        path.append(.addPlayers(gameTypeId: GameTypes.shared.idOf(type: gameType)))
                    } label: {
                        VStack(spacing: 8) {
                            Text(gameType.emoji).font(.system(size: 40))
                            Text(gameType.displayName).font(.headline)
                        }
                        .frame(maxWidth: .infinity, minHeight: 120)
                        .background(Color.skGreen.opacity(0.12))
                        .clipShape(RoundedRectangle(cornerRadius: 20))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(16)
        }
        .navigationTitle("Choose a game")
    }
}

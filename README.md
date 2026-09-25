# Score Keeper — KMP Card Game Scorepad

A Kotlin Multiplatform (KMP) app for tracking scores across card/board games —
Uno, Pool Rummy, Phase 10, and a fully generic "Custom Game" scorepad for
anything else (Spades, Hearts, Yahtzee, your own house game, etc).

**UI is native per platform** — Jetpack Compose on Android, SwiftUI on iOS —
sharing one KMP `shared` module for domain models, scoring rules, local
persistence, and the app-level state/API layer (`AppController`). This is a
deliberate choice over Compose Multiplatform: each platform gets a UI that
looks and feels native, at the cost of writing (and keeping in sync) two UI
layers instead of one.

Community "Events" (multi-sport tournaments — Volleyball, Table Tennis,
Carrom, Badminton, etc. with flexible bracket formats) and account
login/cloud sync (Firebase, phone + email) are designed and wireframed but
**not yet coded** — see "Known gaps" below.

## Project layout

```
ScoreKeeper/
├── shared/                     # KMP module — the only code shared between platforms
│   └── src/
│       ├── commonMain/kotlin/com/scorekeeper/
│       │   ├── domain/Models.kt          # GameType, RoundOutcome, Player, GameSession, GameRules...
│       │   ├── scoring/ScoringEngine.kt  # RummyScoringEngine, UnoScoringEngine, Phase10, Custom
│       │   ├── data/GameRepository.kt    # SQLDelight-backed repository, reactive Flows
│       │   ├── AppController.kt          # Platform-agnostic controller (StateFlow for Android,
│       │   │                              #   callback+Cancellable for Swift)
│       │   └── InteropHelpers.kt         # Swift-safe wrappers around enums & default-arg functions
│       ├── commonMain/sqldelight/        # ScoreKeeper.sq — table schema + queries
│       ├── androidMain/                  # AndroidSqliteDriver
│       └── iosMain/                      # NativeSqliteDriver
├── androidApp/                 # Native Android app (Jetpack Compose, Material 3)
│   └── src/main/kotlin/com/scorekeeper/app/
│       ├── MainActivity.kt
│       └── ui/                 # theme/, nav/NavGraph.kt, screens/ (Home, GamePicker, AddPlayers,
│                                #   ScoreEntry, Summary), App.kt (NavHost wiring)
├── iosApp/                     # Native iOS app (SwiftUI)
│   ├── iosApp.xcodeproj/       # Hand-authored Xcode project (see caveat below)
│   └── iosApp/
│       ├── iOSApp.swift, ContentView.swift (NavigationStack + Route enum), Theme.swift
│       ├── Models/AppViewModel.swift       # ObservableObject bridging AppController
│       └── Views/                          # HomeView, GamePickerView, AddPlayersView,
│                                            #   ScoreEntryView, SummaryView
├── gradle/, gradlew            # Gradle wrapper (pinned to Gradle 8.7)
└── settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml
```

## What's implemented

- **Home**: list of past/ongoing games, tap "New Game" to start, swipe/tap to delete.
- **Game picker**: interactive grid — Uno, Rummy, Phase 10, Custom Game.
- **Add Players**: game name + 2+ player names. For Rummy, also shows
  editable house-rule fields, pre-filled with the values you gave me:
  - Pool limit: **200** (a cumulative total over 200, i.e. 201+, eliminates a player)
  - First drop: **25** · Middle drop: **40** · Full count: **80**
- **Score entry**: running standings + a form to enter each round's score.
  For Rummy, each active player gets Win / First Drop / Middle Drop / Full
  Count chips, which auto-apply the configured penalty (or take a deadwood
  point count you type in for a normal loss). Eliminated players drop out
  of future rounds automatically. Undo removes the last round.
- **Summary**: final standings + winner banner once the game ends.
- **Local persistence**: every session, player, and round score is saved to
  an on-device SQLite DB via SQLDelight — works fully offline on both
  platforms. `GameRepository` is the one seam a future cloud-sync layer
  (Firebase) would plug into, without touching either UI.

## Scoring engines (`shared/.../scoring/ScoringEngine.kt`)

Each game type has its own engine behind one `ScoringEngine` interface:
- **RummyScoringEngine** — cumulative pool scoring with configurable
  drop/middle-drop/full-count penalties and pool-limit elimination.
- **UnoScoringEngine** — points accumulate each hand; first to a target
  score (default 500) "wins" or "loses" depending on a toggle.
- **Phase10ScoringEngine** — lowest cumulative score wins.
- **CustomScoringEngine** — generic running totals, optional target score,
  optional lowest-wins — the "works for any game" fallback.

All the magic numbers (pool limit, penalties, target scores) live in
`GameRules`, a serializable data class stored per-session as JSON, so
house-rule variants don't require code changes.

## How Android and iOS share `shared` without sharing UI

`AppController` (in `shared/commonMain`) is the one class both apps talk to.
It owns a `GameRepository` and exposes every action (`startNewGame`,
`recordRound`, `undoLastRound`, `finishSession`, `deleteSession`) plus two
ways to observe data:

- **Android/Compose** collects `AppController.sessions` — a `StateFlow` —
  directly via `collectAsStateWithLifecycle()`.
- **iOS/SwiftUI** has no first-class `StateFlow` support without extra
  tooling (SKIE etc.), deliberately skipped to keep the shared module
  dependency-free. Instead it calls `watchSessions { ... }` /
  `watchSession(sessionId) { ... }`, which take a plain callback and return
  a `Cancellable`. `AppViewModel` (an `ObservableObject`) wraps that callback
  in a `@Published` property so SwiftUI views just read state normally.

### A note on the Kotlin/Native interop, honestly

I could not run a real Kotlin/Native or Xcode compile in the sandbox this
was built in (see "Known gaps"), so none of the Swift↔Kotlin bridging below
has been verified against an actual generated Objective-C header — it's
built to match Kotlin/Native's documented, standard export conventions, but
"first real Xcode build" is the true check, the same way "first Gradle
sync" is for the Android side.

To keep the riskiest, least-documented part of that bridging (how Kotlin
enum *entries* are name-mangled for Swift, e.g. `RoundOutcome.FIRST_DROP` →
something like `.firstDrop`) out of the app entirely, `InteropHelpers.kt`
routes every enum across the boundary as a plain string id
(`GameType.name` / `RoundOutcome.name`) instead:

- `GameTypes.shared.byId(id:)` / `.idOf(type:)` and `RoundOutcomes.shared`
  do the same for `RoundOutcome` — both are plain Kotlin `object`s, which
  *do* bridge predictably (`Foo.shared` from Swift).
- Two other well-documented K/N gotchas are worked around the same way:
  Kotlin **default parameter values don't cross into the generated Swift
  API** at all (every param must be passed explicitly), so `GameRules()`
  and `GameRepository(driverFactory, ioDispatcher = ...)` each get a
  no-default-args factory function (`defaultGameRules()`, `rummyRules(...)`,
  `createGameRepository(driverFactory:)`); and **top-level Kotlin functions**
  in a file are exported under a `<FileName>Kt` facade class, so Swift calls
  them as `InteropHelpersKt.defaultGameRules()`, not bare.
- `Enum.name` itself *is* a standard, safe property to read from Swift (it's
  not mangled), so `session.gameType.name == "RUMMY"` is used freely for
  comparisons.

If the first real build turns up a naming mismatch, it will be isolated to
`InteropHelpers.kt` and the handful of call sites listed above — nothing
else in the SwiftUI views depends on Kotlin/Native enum interop directly.

## Building — Android

1. Open the `ScoreKeeper/` folder in **Android Studio** (Koala/2024.1+).
2. Let Gradle sync. This needs normal internet access to Google's Maven
   repo, Maven Central, and the Gradle Plugin Portal — I could not run this
   sync inside my sandboxed workspace (those three hosts are blocked
   there), so **this project has not been compiled yet**. Treat first sync
   as the first real build check.
3. Run the `androidApp` configuration on an emulator or device.

## Building — iOS

1. First build `shared` once from the command line so the KMP framework
   exists before Xcode needs it:
   ```
   cd ScoreKeeper
   ./gradlew :shared:embedAndSignAppleFrameworkForXcode
   ```
   (The Xcode project's "Embed Shared KMP Framework" run-script build phase
   does this automatically on every build too — this manual first run is
   just for a faster first debug loop.)
2. Open `iosApp/iosApp.xcodeproj` in Xcode 16+.
3. **Caveat**: this `.xcodeproj` was hand-authored here (no macOS/Xcode
   available in this sandbox to generate or verify one), following Xcode's
   standard project file format and the same run-script/framework-search-path
   setup the official Kotlin Multiplatform Xcode template uses. If Xcode
   reports it needs "resolving" or flags a setting on first open, that's the
   one thing in this repo to sanity-check by hand — everything else
   (the Swift source itself) is ordinary SwiftUI.
4. Select the `iosApp` scheme, pick a simulator, and run.

## Rummy rules reference (as configured)

| Outcome        | Penalty |
|----------------|---------|
| Win            | 0       |
| First drop     | 25      |
| Middle drop    | 40      |
| Full count     | 80      |
| Normal loss    | actual deadwood points (capped at 80) |

A player is eliminated once their cumulative total exceeds 200 (i.e. hits
201+). Game ends when one player remains. All four numbers are editable
per-session on the Add Players screen.

## Known gaps / next steps

- **Neither build has been compiled or run yet.** This sandbox has no
  network access to Maven/Google/Plugin Portal repos and no macOS/Xcode, so
  the first Android Studio Gradle sync and the first Xcode build are both
  genuinely untested — treat them as the real check, not a formality.
- No app icon / launcher icon yet on either platform.
- No cloud sync yet — `GameRepository` behind `AppController` is the seam;
  Firebase (Auth phone-OTP + email, Firestore, last-write-wins by an
  `updatedAtMillis` field) was the plan discussed but is not implemented.
- No automated tests yet — the scoring engines are pure functions and are
  easy to unit test; `shared/src/commonTest` is wired up in the Gradle
  config and ready for test classes.
- Community "Events" (multi-sport tournaments, brackets, draws) exist only
  as clickable wireframes, not code.
- AdMob / Remote-Config-driven ad placements: planned, not implemented.

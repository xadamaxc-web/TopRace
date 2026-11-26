# TOP RACE Tracker — v1.0 (Session 3)

Local-only Android overlay. Kotlin + Compose + Room. No server, no OCR.

## Build
- Push to GitHub repo `TopRace-Tracker` (branch `main`).
- Actions → **Build APK** → download artifact `TopRace-debug-apk`.
- Local: Android Studio (JDK 17) → open folder → Run. CLI: `gradle assembleDebug` (Gradle 8.9; no wrapper jar included).

## Run
1. Install APK → open app → **Grant overlay permission** → back → **Start overlay**.
2. Overlay: 45% height, 5% from top. Drag by the TOP RACE title. ✕ closes.
3. Pick 1 road + 5 cars (numbered by pick order) → **ANALYZE** turns green.

## Session 1 contents
- `Constants.kt` cars/roads/odds/tiers · `SpeedDatabase.kt` 6 road tables
- `SpriteSlicer.kt` + `res/drawable-nodpi/sprite_vehicles.webp` (3x3, 400x300 cells), `sprite_roads.webp` (3x2, 390x78 cells)
- `db/` Room: RoundEntity, StrategySettings, LearnedStats + DAOs
- `OverlayService.kt` foreground service + ComposeView host
- `ui/SetupScreen.kt` Setup UI (balance, max bet, slots, HUD, roads, vehicles)

## Assumptions
Fast roads = Highway, Expressway, Desert. Slot is optional for Analyze.

## Session 2 additions
- `logic/` MathEngine (rev x3 + hidden avg x6), DataEngine (Bayesian, k=10), BetEngine (60/30/10 · 50/30/20 above 5.0x), PatternEngine (streak / hot), AnalysisEngine
- `ui/AnalysisScreen.kt` 2-page pager (Math/Static/Data ranks · Final picks + bets + pattern box) + W1/W2/W3/LOSE bar
- `ui/StrategySettingsPanel.kt` long-press the title (600 ms, no drag) → multipliers, slot biases, data-vs-math slider, saved to Room
- Unit tests: `app/src/test/.../LogicTest.kt` (CI runs `testDebugUnitTest` before assemble)

## Notes
- Fast/Struggle road multipliers and slot biases scale **Fast-tier cars only** (a uniform multiplier would never change a rank).
- Math and data scores are normalized to 0–1 before blending.
- W1/W2/W3/LOSE only highlight for now; Session 3 wires them to the Post-Race screen.

## Session 3 additions
- Post-Race screen (W1/W2/W3/LOSE → 5 s timer, log up to 2 hidden roads, winner odd, SAVE)
- SAVE = insert round + Learner (wins/totalRounds per car+road) in one transaction → HUD updates via Room Flow → balance update → reset
- Balance net = bet on winning pick x winner odd − total stakes (LOSE = −total stakes). W1–W3 need the odd; LOSE needs the winner (pick from the 2 non-picked cars), odd optional
- HUD: Fast road+Fast car 🟢 · Fast road+Medium 🟡 · Struggle road+Fast car 🔴 · Struggle road+Medium 🟡 (assumed)
- No learned rounds for the picked cars on this road → pure math (blend ignored)
- In-progress round + balance persisted (SharedPreferences) and restored on service restart; service is START_STICKY
- Minimize (—/▢ in title bar), haptics on picks/outcome/save
- CI: tag `v*` → GitHub Release with the APK

## Release
`git tag v1.0.0 && git push origin v1.0.0`

## S9b Stage 1 — five-car observer prototype
- Open the app and choose **Start screen observer** to request Android MediaProjection consent.
- The observer uses bundled on-device ML Kit OCR; no cloud requests are made.
- It reports recognized car names and visible road names in its ongoing notification.
- Screen 1 odds are deliberately ignored; authoritative A/B odds belong to Screen 2.
- See `CHANGES_S9b_STAGE1.md` for scope, test steps, and current limitations.

# S9b Stage 1 — Screen 1 observer foundation

## Scope
- Five-car version only.
- Adds a user-started Android MediaProjection screen observer.
- Runs bundled ML Kit Latin text recognition on-device.
- Recognizes the five currently relevant car names: THUNDER, EAGLE, WHALE, DRAGON, POWER.
- Recognizes known road names if visible in OCR text.
- Reports recognition state through an ongoing notification and stores the latest structured recognition summary in private SharedPreferences.
- Frames are processed in memory and are not written to disk.
- No odds are extracted from Screen 1.
- Does not click, tap, bet, or control the game.
- Does not alter S9a Room entities, migrations, prediction engines, or settlement logic.

## How to test
1. Upload this complete project to the existing GitHub repository (replace the project files from this ZIP).
2. Run Actions → Build APK and download `TopRace-debug-apk`.
3. Install/update the APK.
4. Open TOP RACE Tracker → Start screen observer.
5. Approve Android's screen-sharing prompt.
6. Open the five-car game Screen 1. Check the ongoing notification for recognized car count and any road name found.
7. Stop screen observer before ending the test.

## Current limitations (intentional prototype boundary)
- This stage verifies capture + offline OCR and basic name extraction. It is not yet the final validated Screen 1 classifier.
- Fixed crop regions, road-marker visual/template recognition, confidence thresholds, countdown parsing, and round-state synchronization are not yet implemented.
- Recognition summary is diagnostic only; it is not inserted into the historical rounds database.
- Screen 2 authoritative A/B odds and road-length extraction, and Screen 3 winner matching are not implemented in this stage.
- A local Android SDK/Gradle installation was not available in the editing environment, so this ZIP has not been compiled here. GitHub Actions must provide the first build result.

## Diagnostic log update
- Main screen now displays the latest 40 observer diagnostic events.
- Events include service start/stop, capture permission, virtual-display startup, OCR results, and capture/OCR failures.
- Use **Refresh diagnostic log** after stopping the observer to inspect the latest events.
- This log is local app diagnostic text only; it does not create or claim to save game rounds.
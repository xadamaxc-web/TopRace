# Session 8
- Every saved round now also stores each version's FULL ordering of the 5 cars, best first
  (e.g. `Eagle|Jeep|Shark|Ape|Whale`) for the 4 versions that change over time: MATH, DATA, BLEND, RECENT.
- BASE (shortest odds first) is not saved: it never changes, and tools/analyze_toprace.py rebuilds it from the 5 cars.
- DB v3 migration adds 4 text columns (order_math, order_data, order_blend, order_recent). Existing rounds are kept; their new columns stay empty.
- CSV export: 4 new columns appended at the END (order_math, order_data, order_blend, order_recent). Earlier column positions are unchanged.
- tools/analyze_toprace.py: new section 5 (integrity check of saved order vs saved winner rank,
  where the winner sat 1st..5th in each version, how often versions agree on #1). Rounds without orderings are skipped.
- Tests added: ordering text holds all 5 cars best-first; saved order matches the winner's rank.
- versionCode 7 / versionName 1.0.4. Same package and debug key: installs over S7 as an update.

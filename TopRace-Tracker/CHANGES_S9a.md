# Session 9a (built on S8)
- New Setup screen (from the mock): slots 1-3 top to bottom, roads enabled after a slot, 3x3 cars with Copy / Mine toggle,
  5-car strip (trophy on my 1st), ANALYSE locks my own order with the time. 3 rows x 20 dots with 5 pages (BLEND W/L, MATH rank,
  confidence band, MINE rank, winner odd); a white ring marks SURE rounds.
- Analysis screen has 4 pages: Ranks (adds a MINE row and a LIVE row), Picks, Roads, History.
  Roads page: 3 blocks = the mini-map (drag the 2 separators, tap a block to set its road). LIVE ranks the 5 cars by total time over the 3 roads and lengths.
  History page: winners with road/slot, odd (star >= 10) and BLEND / MINE results; filter to this road + slot.
- Result screen: tap the winner, enter the winner's final odd with the 1 / 5 / . / backspace keypad (required), SURE, stake (none/20/33/50/75/ALL).
  W1/W2/W3/LOSE shown for MATH, BASE, DATA, BLEND, RECENT, LIVE and MINE.
- DB v4 (migration keeps all rounds): road per slot, 3 lengths, my order + lock time + rank, SURE, stake, LIVE rank/order.
- CSV has all new columns. tools/analyze_toprace.py also scores LIVE and MINE.
- Not in this build: screen capture / automatic reading (S9b).

# Encounter acquisition results (Sprint 4, #439)

The Event screen uses the settled `ChanceEncounterSession` snapshot. After a successful choice,
`ChanceAcquisitionSession` shows a read-only result overlay. Continue completes the Event once;
Close returns to the already-settled Outcome page. An empty or failed result retains the existing
Outcome and Continue flow. Neither button applies the reward again.

The overlay lists the actual signed HP and Gold changes, acquired card and item counts, and the
configured Curse effect descriptions. Abandoned Mine can produce Gold alone or Gold plus one
Curse; the overlay follows the committed outcome in either case. Shop uses the same view after a
successful purchase receipt. Its Continue and Close actions dismiss the overlay without buying
again.

Mysterious Shrine places four configured choices in separate full-width rows so that the long
Curse descriptions remain legible in the 1280 × 800 game scene. The two-choice presentation
keeps its original framed layout.

## Verification

- `ChanceEncounterDisplayTest`: four Shrine choices have separate bounds inside the scene.
- `ChanceCurseChoiceDispatchTest`: both Shrine Curses and both Mine Curses display settled Gold
  plus the granted item, with no second item grant. A Mine roll without a Curse displays Gold only.
- `AcquisitionResultDisplayTest`: mixed results and signed `Gold +60` / `HP -8` display, and
  dismissal leaves result data unchanged.
- `ShopDisplayTest`: a successful purchase opens the result overlay without charging twice.

Manual game checks still need to confirm the layout and animations at the target screen size,
including Enter Demo Event → Shrine, Mine with both random outcomes, and Shop purchase.

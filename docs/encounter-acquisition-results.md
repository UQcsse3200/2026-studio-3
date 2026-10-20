# Encounter acquisition results (Sprint 4, #439)

The Event screen uses the settled `ChanceEncounterSession` snapshot. After a successful choice,
`ChanceAcquisitionSession` shows a read-only result overlay. Continue completes the Event once;
The visible result view now provides Continue only. It plays presentation-only acquisition feedback
before completing the existing flow. An empty or failed result retains the existing Outcome and
Continue flow. Neither presentation nor animation applies the reward again.

The overlay lists the actual signed HP and Gold changes, acquired card and item counts, and the
configured Curse effect descriptions. Abandoned Mine can produce Gold alone or Gold plus one
Curse; the overlay follows the committed outcome in either case. Shop uses the same view after a
successful purchase receipt. Continue dismisses the overlay without buying again.

## Acquisition presentation refinement (20 October)

- All Event healing result tiles advertise the configured heal (for example `HP +20`), including
  at full health. There is no full-health message. The original outcome applier still clamps
  actual HP to the player's current maximum, including increases to that maximum. Gold and
  damage displays retain committed amounts. Presentation and Continue never apply healing.

- Card entries use a clickable `CARD REWARD` tile with artwork-only thumbnail and `click to check`.
  The tile opens all committed card entries/copies in a read-only full-card grid. Back restores
  the same result panel; it does not select, grant or complete anything. Continue is ignored while
  the preview is open. Card frames/art are loaded before constructing shared CardWidgets.
- Manual check: Healer Bandage result -> thumbnail -> full card -> bottom-right Back -> same result
  -> Continue. Multi-card/copy browsing and repeated Back are covered by regression tests.

- Reuses the existing mythic reward panel, reward frames, Continue artwork and category icons.
- Preserves the live event/shop background beneath a translucent dimmer; no common background is loaded.
- Presents entries in a centered, read-only grid with signed currency/health changes.
- Continue starts staggered category-icon flights with pixel trails and destination feedback.
- Repeated Continue input is ignored. HP changes use the map HUD's `images/heart.png`.
  Resource losses and plain effects do not fly as gains; newly owned items, including Curse items,
  fly to the item destination without reapplying their reward or effect.
- Disposal cancels pending presentation actions and releases the view's managed UI assets.
- Known items now reuse `ItemArtwork.cutoutPath` from the Library/inventory artwork system.
  Gold uses the existing coin pouch, and the left destination uses the existing coin icon.
- Icon drawable dimensions are retained for `Scaling.fit`; only frame/button drawable minimum
  sizes are cleared. Flights measure the displayed reward artwork and destination centres in the
  same overlay coordinate system, rather than starting at the surrounding tile centre.
- The title is inside the panel below the compass, with reserved header and footer space.
  The combined Gold + Brand of the Damned case was inspected in the running game; further
  layout tightening was followed by automated regression checks.

Mysterious Shrine uses three compact equal-width choices across the first row, with Leave centred
below. Choice numbering is removed, and illustrated-button labels are optically aligned with their
inner frames. Result narration uses a text-sized, symmetric feathered shade; Mine has no outer
result frame, and Healer/Fountain use a lighter shade.

Shop purchases do not interrupt shopping. Leave skips results when nothing was purchased, otherwise
opens SHOP RESULTS with all committed purchases. Card rewards share one clickable thumbnail;
read-only previews show three cards per page with Previous/Next and Back. Signed gold changes fly
from their result artwork to the left gold icon without charging or granting currency again.

Generated prices: Common 8–12, Uncommon 15–20, Rare 25–30 gold, before existing discounts.
Refresh fees are free, 2, 4, 8, 16 and continue doubling. Demo Shop and Demo Event entry points,
picker and isolated screens have been removed; production map nodes use the same retained UI.

## Verification

- `ChanceEncounterDisplayTest`: four Shrine choices have separate bounds inside the scene.
- `ChanceCurseChoiceDispatchTest`: both Shrine Curses and both Mine Curses display settled Gold
  plus the granted item, with no second item grant. A Mine roll without a Curse displays Gold only.
- `AcquisitionResultDisplayTest`: mixed results and signed `Gold +60` / `HP -8` display, and
  dismissal leaves result data unchanged.
- `ShopDisplayTest`: purchases update the balance without opening results; Leave displays the
  collected purchases without charging twice, or completes immediately with no purchases.
- `ShopRefreshIntegrationTest`: real player/deck APIs preserve purchases, refreshed prices and
  remaining gold across leaving and restoring the player for the next node.

Final local validation after demo removal: 2615 tests passed, desktop compilation and format checks
passed. This was automated and code-path verification, not a manual traversal of every map event.

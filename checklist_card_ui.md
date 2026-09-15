# Card Upgrade and Dynamic Card UI Implementation Checklist

**Owner:** Raymond (@ataipham)  
**Feature:** Per-instance card upgrades and dynamic card UI  
**Prepared:** 10 September 2026  
**Project:** Team 6 - Cards/Library  
**Status:** Checkpoints A, B and C passed; cross-team confirmations, production acquisition and persistence integration remain pending  

This document is the source-of-truth checklist for implementing per-instance card upgrades, dynamic Scene2D card rendering, hover details, and pixel-art asset migration. Update the checkboxes, evidence links, decisions, blockers, and dates as the task progresses.

## Status legend

- `[x]` Confirmed or completed with evidence.
- `[ ]` Not started or not yet proven complete.
- `BLOCKED` Cannot continue until the recorded dependency is resolved.
- `IN PROGRESS` Work has started but the acceptance criteria are not yet satisfied.

Do not mark an implementation item complete merely because code was written. It is complete only when the relevant tests, integration checks, and documentation also pass.

## Confirmed design decisions

- [x] Upgrading applies to one individual card copy, not every card with the same `cardId`.
- [x] Every owned card copy will have its own stable `instanceId`.
- [x] `CardInstance` is owned and implemented by Raymond for this task.
- [x] Base and upgraded versions use the same artwork, frame, and layout.
- [x] The UI distinguishes upgraded cards using dynamic presentation such as a `+` marker, colour treatment, or upgrade overlay.
- [x] Card artwork will be pixel art instead of the current 3D/full-card images.
- [x] The target in-game card-widget size is `225 x 456`.
- [x] Gameplay information must not be baked into the PNG artwork.
- [x] Name, rarity, energy cost, description, and other displayed fields are rendered dynamically from card data.
- [x] An upgrade may change `name`, `description`, `cost`, `rarity`, `effects[].value`, and `effects[].duration`.
- [x] An upgrade will initially keep the base `id`, `type`, `target`, and `texturePath`.
- [x] Card play and card upgrade operations identify a specific copy using `instanceId`.
- [x] `cardId` remains the stable key used to retrieve the shared `CardConfig` from `CardService`.
- [x] UI and gameplay must consume the same resolved card values.
- [x] The intended final content is 26 card definitions, 26 shared base/upgrade artworks, and one upgrade configuration per card, rather than 52 unrelated definitions.

## Responsibility boundary

Raymond will implement and maintain `CardInstance` and coordinate all changes required by this task. The following boundaries still apply even when Raymond performs the integration work:

| Area | Primary responsibility |
| --- | --- |
| `CardConfig`, `CardUpgradeConfig`, JSON loading and validation | Team 6 / this task |
| `CardInstance`, instance identity and upgrade level | Raymond / this task |
| Player deck, draw pile, hand and discard integration | Coordinate with Team 5; implemented in this task where required |
| Operation that upgrades a selected `instanceId` | Card-upgrade feature; implemented or integrated through this task |
| `CardWidget`, battle-hand UI and details tooltip | Card UI work in this task |
| Enemy/player effect application | Existing combat, enemy and player owners |
| Shop/reward acquisition | Existing shop/reward owners; this task supplies the instance-creation contract |

Any cross-team API change must be recorded and communicated before dependent code is merged.

## Target architecture

```text
cards.json
  -> CardConfig + CardUpgradeConfig
  -> CardService lookup by cardId

CardInstance
  -> instanceId
  -> cardId
  -> upgradeLevel

CardConfig + CardInstance
  -> CardResolver
  -> ResolvedCard
       |-> CardWidget
       |-> CardDetailsTooltip
       `-> CardPlayService / effect resolution
```

The renderer and gameplay system must never calculate upgraded values independently. A value shown by `CardWidget` must be the same value used by `CardPlayService`.

---

# Phase 1 - Confirm and record the integration contract

## Step 1 - Record the agreed design

- [x] Copy the confirmed decisions above into the relevant task ticket or team decision record.
- [ ] Inform Team 5 that deck and card-play interfaces will move from identifying copies by `cardId` to identifying copies by `instanceId`.
- [ ] Inform the upgrade-feature owner that upgrade operations must receive a specific `instanceId`.
- [x] Confirm that `upgradeLevel` supports only `0` and `1` for the current sprint.
- [x] Confirm that an upgrade keeps the base card's `type`, `target`, and artwork.
- [ ] For each affected cross-team contract (deck storage, card-play request, upgrade action, and acquisition), record the implementation owner, consuming team, and real issue/PR link; one entry per contract is sufficient, not one per method.

**Phase 1 completion evidence:**

- Decision record or ticket link: [Member 2 task ticket](member2_task.md)
- Team 5 confirmation:
- Upgrade-feature confirmation:
- Notes/blockers: API ownership is recorded in the task ticket; real issue/PR links remain pending.

---

# Phase 2 - Extend the card configuration schema

## Step 2 - Create `CardUpgradeConfig`

- [x] Create `cards/configs/CardUpgradeConfig.java`.
- [x] Add a public no-argument constructor for libGDX JSON deserialization.
- [x] Add `name`.
- [x] Add `description`.
- [x] Add `cost`.
- [x] Add `rarity`.
- [x] Add `EffectConfig[] effects`.
- [x] Document that `id`, `type`, `target`, and `texturePath` are inherited from the base card.

## Step 3 - Attach upgrade data to `CardConfig`

- [x] Add an optional `CardUpgradeConfig upgrade` field to `CardConfig`.
- [x] Document the migration behaviour when `upgrade == null`.
- [x] Preserve all existing base-card fields and JSON compatibility.
- [x] Decide when the final validator will change from allowing missing upgrades to requiring an upgrade for every official card.

## Step 4 - Add the first representative upgrade

- [x] Add a valid `upgrade` object to the Strike definition in `cards.json`.
- [x] Keep Strike's `id`, `type`, `target`, and `texturePath` unchanged.
- [x] Give the upgraded version a dynamic display name such as `Strike+`.
- [x] Define and document its upgraded cost, effect value, duration, rarity, and description.
- [x] Do not add all remaining upgrades until the Strike vertical slice is proven.

**Phase 2 completion evidence:**

- Config/model commits: `c36d58e` (`CardUpgradeConfig`) and `ab6c8de` (Strike+ configuration and model tests).
- Example Strike JSON: [Official card configuration](../source/core/assets/configs/cards.json)
- Review notes: Strike+ reuses Strike's ID, type, target and artwork; its initial upgrade is cost 1, Common rarity and 12 immediate damage.
- Regression evidence: `./gradlew.bat --no-daemon core:test --tests "com.csse3200.game.cards.*"` completed with 199 tests, 0 failures, 0 errors and `BUILD SUCCESSFUL` on 11 September 2026.

---

# Phase 3 - Update loading and validation

## Step 5 - Validate upgrade configuration

- [x] Extend `CardValidator` to validate `CardUpgradeConfig` when present.
- [x] Reject null or blank upgrade names.
- [x] Reject null or blank upgrade descriptions.
- [x] Reject negative upgrade costs.
- [x] Reject null upgrade rarity.
- [x] Reject null or empty upgrade-effect arrays.
- [x] Reject null entries inside the upgrade-effect array.
- [x] Validate each upgrade effect's type, value, and duration.
- [x] Validate upgrade effects against the base card's target.
- [x] Produce precise error paths such as `upgrade.effects[0].value`.
- [x] Avoid mutating the base or upgrade configuration during validation.

## Step 6 - Verify JSON loading

- [x] Confirm `CardConfigLoader` deserializes a nested `upgrade` object.
- [x] Test a valid upgraded card.
- [x] Test temporary backward compatibility for a card without `upgrade`.
- [x] Test an upgrade with missing fields.
- [x] Test an upgrade with a negative cost.
- [x] Test invalid upgrade effect values.
- [x] Test invalid upgrade effect durations.
- [x] Test malformed nested upgrade JSON.
- [x] Confirm existing base-card loading tests still pass.

**Phase 3 completion evidence:**

- Loader/validator test output: All focused Phase 3-5 tests passed; the full card suite passed 199/199 tests on 11 September 2026.
- Error-message examples: `upgrade.name must not be blank`, `upgrade.effects[0].value must be positive`, and `upgrade.effects[0].duration must be positive for POISON`.
- PR/commit: `ecb7838` (loader/required nested fields), `dc31dd8` (upgrade validation), and `74a3e09` (formatting).

---

# Phase 4 - Implement runtime card identity

## Step 7 - Create `CardInstance`

- [x] Create `cards/runtime/CardInstance.java`.
- [x] Store `instanceId`.
- [x] Store `cardId`.
- [x] Store `upgradeLevel`.
- [x] Reject null, empty, or blank `instanceId`.
- [x] Reject null, empty, or blank `cardId`.
- [x] Restrict `upgradeLevel` to `0` or `1` for this sprint.
- [x] Add `isUpgraded()`.
- [x] Add an immutable upgrade operation that keeps the same `instanceId` and `cardId`.
- [x] Document that a `CardInstance` is a runtime data object, not necessarily an ECS entity.

## Step 8 - Create `CardInstanceFactory`

- [x] Create `cards/runtime/CardInstanceFactory.java`.
- [x] Add `create(String cardId)` for a normal instance.
- [x] Add `createUpgraded(String cardId)` if useful for tests/debugging.
- [x] Generate a unique stable `instanceId`, for example with UUID.
- [x] Keep explicit-ID construction available for deterministic tests.
- [x] Ensure the factory validates the requested `cardId` through the agreed registry/service boundary.

## Step 9 - Test runtime identity

- [x] Test a valid normal instance.
- [x] Test a valid upgraded instance.
- [x] Test that two copies of Strike have different `instanceId` values.
- [x] Test that upgrading preserves `instanceId`.
- [x] Test that upgrading preserves `cardId`.
- [x] Test that upgrading changes the level from `0` to `1`.
- [x] Test invalid instance IDs.
- [x] Test invalid card IDs.
- [x] Test invalid upgrade levels.

**Phase 4 completion evidence:**

- Runtime-model tests: 11 `CardInstance` and `CardInstanceFactory` tests passed, including duplicate-card identity and immutable upgrade checks.
- PR/commit: Pending; Phase 4 runtime files and tests are currently in the working tree.

---

# Phase 5 - Resolve base and upgraded card values

## Step 10 - Create `ResolvedCard`

- [x] Create `cards/runtime/ResolvedCard.java`.
- [x] Include `instanceId`.
- [x] Include `cardId`.
- [x] Include resolved `name`.
- [x] Include resolved `description`.
- [x] Include resolved `cost`.
- [x] Include resolved `type`.
- [x] Include resolved `rarity`.
- [x] Include resolved `target`.
- [x] Include an immutable/copy-safe list of resolved effects.
- [x] Include `texturePath`.
- [x] Include `upgraded`.
- [x] Ensure consumers cannot mutate the library's base configuration through this object.

## Step 11 - Create `CardResolver`

- [x] Create `cards/runtime/CardResolver.java`.
- [x] Add `resolve(CardConfig config, CardInstance instance)`.
- [x] For level `0`, select base display and gameplay fields.
- [x] For level `1`, select upgrade name, description, cost, rarity, and effects.
- [x] Always inherit base `id`, `type`, `target`, and `texturePath`.
- [x] Reject a mismatch between `instance.cardId` and `config.id`.
- [x] Reject an upgraded instance when `config.upgrade` is absent.
- [x] Copy effect data so resolving never mutates the source configuration.
- [x] Keep this resolver as the single source of final card values.

## Step 12 - Test resolution

- [x] Confirm normal Strike resolves to its base cost and damage.
- [x] Confirm upgraded Strike resolves to its upgraded cost and damage.
- [x] Confirm both versions use the same texture path.
- [x] Confirm the upgraded display name is used.
- [x] Confirm base `CardConfig` remains unchanged after resolution.
- [x] Confirm resolved effect collections cannot modify the source effect array.
- [x] Test null arguments.
- [x] Test mismatched card IDs.
- [x] Test a missing upgrade configuration.

## Checkpoint A - Data model

- [x] **PASS:** Strike base and Strike upgrade load, validate, and resolve correctly.
- [x] **PASS:** Resolving an upgrade does not mutate the base configuration.

**Checkpoint A evidence:**

- Test command/result: `./gradlew.bat --no-daemon core:test --tests "com.csse3200.game.cards.*"` — 199 tests, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESSFUL` on 11 September 2026. `./gradlew.bat --no-daemon spotlessJavaCheck` also passed.
- Commit/PR: Phase 2-3 commits `c36d58e`, `ab6c8de`, `ecb7838`, `dc31dd8`, and formatting commit `74a3e09`; Phase 4-5 files await commit.
- Notes: Strike and Strike+ share `cardId`, type, target and texture. `ResolvedCard` takes defensive copies on construction and access, so consumer mutations cannot affect either the resolved snapshot or source configuration.

Do not migrate deck classes until Checkpoint A passes.

---

# Phase 6 - Migrate `PlayerDeck` to instances

## Step 13 - Change the internal collection

- [x] Replace `List<String> cardIds` with `List<CardInstance> cards`.
- [x] Accept existing instances safely via `PlayerDeck.fromInstances(service, instances)`; retain string constructors for compatibility (Java generic-erasure constraint).
- [x] Prevent duplicate `instanceId` values inside one player deck.
- [x] Continue allowing multiple instances with the same `cardId`.

## Step 14 - Add instance-aware APIs

- [x] Add `addCard(CardInstance card)`.
- [x] Add `removeCard(String instanceId)`.
- [x] Keep or update `removeCardAt(int index)` to return `CardInstance`.
- [x] Add `getCard(String instanceId)` returning `Optional<CardInstance>`.
- [x] Add `containsInstance(String instanceId)`.
- [x] Add `countByCardId(String cardId)`.
- [x] Add `getCards()` returning an immutable snapshot.
- [x] Ensure all methods clearly distinguish `instanceId` from `cardId`.

## Step 15 - Provide temporary compatibility where required

- [x] Audit every current caller of `addCard(String)` and `getCardIds()`.
- [x] If required, retain `addCard(String cardId)` temporarily and create an instance through `CardInstanceFactory`.
- [x] If required, retain `getCardIds()` as a deprecated compatibility snapshot.
- [x] Keep the new instance APIs independent of deprecated ID-only APIs; allow only the documented transitional adapters for existing production callers until Checkpoint C.
- [x] Record when compatibility methods can be removed.

## Step 16 - Preserve identity when copying

- [x] Update `copy()` to create an independent deck collection.
- [x] Preserve each card's `instanceId`, `cardId`, and `upgradeLevel` in the copy.
- [x] Confirm modifying one deck collection does not modify the other.

## Step 17 - Update `PlayerDeckTest`

- [x] Test multiple instances with the same `cardId`.
- [x] Test rejection of duplicate `instanceId` values.
- [x] Test removal by `instanceId`.
- [x] Test that upgrading one copy does not upgrade another.
- [x] Test counting by `cardId`.
- [x] Test immutable snapshots.
- [x] Test `copy()` identity and independence.
- [x] Update all existing PlayerDeck regression tests.

**Phase 6 completion evidence:**

- PlayerDeck tests: `PlayerDeckTest`, `PlayerDeckFactoryTest`, and `DeckIdentityTest`; immutable snapshots, copy isolation, duplicate identity rejection, and upgrading/removing a selected copy are covered.
- Compatibility decisions: [Deck identity contract](docs/card-deck-identity.md). Legacy acquisition creates fresh instances; legacy gameplay explicitly selects base-only copies. Removal APIs never guess a cardId.
- PR/commit: Working-tree changes; not committed or pushed.

---

# Phase 7 - Migrate `BattleDeck` to instances

## Step 18 - Change all combat piles

- [x] Change `drawPile` to `List<CardInstance>`.
- [x] Change `hand` to `List<CardInstance>`.
- [x] Change `discardPile` to `List<CardInstance>`.
- [x] Construct the battle deck from `PlayerDeck.getCards()`.
- [x] Preserve instances rather than generating new IDs at battle start.

## Step 19 - Update draw and snapshot APIs

- [x] Change `drawOne()` to return `CardInstance`.
- [x] Change `drawCards(int)` to return `List<CardInstance>`.
- [x] Change `getDrawPile()` to return an immutable instance snapshot.
- [x] Change `getHand()` to return an immutable instance snapshot.
- [x] Change `getDiscardPile()` to return an immutable instance snapshot.

## Step 20 - Play and discard by instance

- [x] Change `playCard` to identify the selected copy by `instanceId`.
- [x] Change `discardCard` to identify the selected copy by `instanceId`.
- [x] Remove only the matching instance from the hand.
- [x] Preserve the instance's upgrade level when moving it to discard.
- [x] Ensure `discardHand()` moves the original instances.
- [x] Ensure reshuffling moves the original instances.

## Step 21 - Update `BattleDeckTest`

- [x] Test a hand containing normal Strike, upgraded Strike, and another normal Strike.
- [x] Play the upgraded Strike by `instanceId`.
- [x] Confirm only that instance enters the discard pile.
- [x] Confirm the two normal instances remain unchanged.
- [x] Confirm draw preserves upgrade level.
- [x] Confirm discard preserves upgrade level.
- [x] Confirm reshuffle preserves identity and upgrade level.
- [x] Confirm battle-deck changes do not mutate the source player deck.

## Checkpoint B - Deck identity

- [x] **PASS:** Three Strike copies have three stable, unique instance IDs.
- [x] **PASS:** Only one chosen Strike can be upgraded.
- [x] **PASS:** Instance identity survives draw, play, discard, and reshuffle.

**Checkpoint B evidence:**

- Test command/result: Run from `source/` with Microsoft JDK 21.0.8: `.\gradlew.bat --no-daemon :core:test` — 841 tests, 0 failures, 0 errors, 0 skipped (including 220 card tests). `.\gradlew.bat --no-daemon spotlessJavaCheck` and `git diff --check` pass. Verified 15 September 2026.
- Evidence: [DeckIdentityTest](source/core/src/test/com/csse3200/game/cards/deck/DeckIdentityTest.java) contains 14 identity/compatibility tests; `SprintTwoCardsIntegrationTest` also verifies both legacy gameplay paths reject an upgraded-only hand without spending energy or applying base effects.
- Commit/PR: Working-tree changes; cross-team review/sign-off still pending.
- Notes (historical B snapshot; the battle bridges/UI exclusion below were superseded by Checkpoint C): Checkpoint B covers runtime deck identity, not upgraded gameplay/UI or save/load. Battle instances snapshot the player deck at battle creation. Legacy BattleScreen temporarily skips upgraded cards; Checkpoint C/D will connect resolved gameplay/presentation. Current ID-only save format loses identity and upgrade state; persistence migration remains a dependency. See [compatibility/removal gates](docs/card-deck-identity.md).

---

# Phase 8 - Update starter decks and acquisition

## Step 22 - Update `PlayerDeckFactory`

- [x] Replace the starter deck's string entries with separately created `CardInstance` objects.
- [x] Give every starter card a unique `instanceId`.
- [x] Start every starter card at `upgradeLevel = 0`.
- [x] Preserve the intended number and order of starter cards.
- [x] Update factory tests.

## Step 23 - Define the shop/reward creation contract

- [x] Keep shop/reward offers keyed by `cardId` when they represent card types.
- [x] Define/test that acquisition creates a fresh `CardInstance` through `CardInstanceFactory`; production shop/reward wiring is tracked below.
- [x] Define/test adding the acquired instance to `PlayerDeck`.
- [x] Verify the acquisition contract creates distinct IDs for two copies (`InstanceCardPlayTest`). Production purchase wiring remains an external dependency.
- [x] Confirm acquisition never reuses another owned card's instance ID.
- [x] Record any external shop/reward dependency that cannot be completed in this task.

**Phase 8 completion evidence:**

- Starter-deck tests: `PlayerDeckFactoryTest`; factory explicitly creates distinct base instances in the original order.
- Acquisition integration evidence: `InstanceCardPlayTest.acquisitionCreatesNewIdentityForEachCopyWithoutChangingOfferId`. Contract is implemented/tested; production inventory-backed shop/reward binding is not migrated. See [acquisition boundary](docs/card-play-instance-migration.md).
- PR/commit: Working-tree changes; external acquisition owners and sign-off remain pending.

---

# Phase 9 - Move card-play requests to `instanceId`

## Step 24 - Standardise `CardPlayRequest`

- [x] Audit the repository for duplicate or legacy `CardPlayRequest` classes.
- [x] Identify and document the production request type.
- [x] Consolidate or clearly deprecate redundant request types.
- [x] Replace the request's card-copy identifier with `instanceId`.
- [x] Preserve the target information required by the existing battle flow.
- [x] Validate null or blank instance IDs.

## Step 25 - Update UI and battle event payloads

- [x] Change card click/drag payloads from `(cardId, targetId)` to `(instanceId, targetId)`.
- [x] Update `BattleActions` to receive `instanceId`.
- [x] Resolve the selected instance before retrieving its shared config.
- [x] Update logging to include both `instanceId` and `cardId` where useful.
- [x] Update event tests and integration adapters.

**Phase 9 completion evidence:**

- Request/event tests: `CardPlayRequestTest`, `BattleActionsTest`, `DragNDropTest`, `Team3CardPlayAdapterTest`, and the upgraded UI-event test in `InstanceCardPlayTest`.
- Duplicate-class decision: Keep `cards.play.CardPlayRequest` / `CardPlayResult`; remove `cards.CardPlayRequest` / `cards.effects.CardPlayResult`. UI event payloads use instanceId and hand events carry CardInstance snapshots.
- PR/commit: Working-tree changes; breaking [cross-team API migration](docs/card-play-instance-migration.md) requires downstream review.

---

# Phase 10 - Use resolved cards during gameplay

## Step 26 - Update `CardPlayService`

- [x] Receive or extract `instanceId` from the card-play request.
- [x] Find the exact `CardInstance` in the current hand.
- [x] Retrieve its `CardConfig` by `cardId`.
- [x] Resolve it through `CardResolver`.
- [x] Validate the resolved target.
- [x] Check affordability using the resolved cost.
- [x] Spend the resolved cost exactly once.
- [x] Execute the resolved effects.
- [x] Move the exact instance to discard after successful resolution.
- [x] On expected failure, spend no energy and move no card.
- [x] On unexpected failure after spending energy, restore the resolved cost.

## Step 27 - Update playability checks

- [x] Change `canPlay` to identify the card copy by `instanceId`.
- [x] Return false when the instance is not in hand.
- [x] Return false when the shared definition is missing.
- [x] Return false when upgrade data is invalid or missing.
- [x] Use the resolved cost for affordability.
- [x] Use the resolved target rules.

## Step 28 - Update effect resolution

- [x] Add a `ResolvedCard` entry point to `CardEffectResolver` or `CardEffectResolutionService`.
- [x] Move production card play to the resolved-card entry point.
- [x] Ensure effects come from `ResolvedCard`, not from a newly fetched base config.
- [x] Preserve configured effect execution order.
- [x] Update unit and integration tests for resolved effects.
- [x] Remove or deprecate unsafe paths only after all callers migrate.

## Step 29 - Update card-play results and diagnostics

- [x] Include `instanceId` in the result.
- [x] Include `cardId` in the result where useful.
- [x] Record the actual resolved energy cost.
- [x] Record the resolved effects/result.
- [x] Preserve target and deck snapshots.
- [x] Make failures identifiable when several copies share one card ID.

## Step 30 - Add end-to-end gameplay tests

- [x] Put a normal Strike and upgraded Strike in the same hand.
- [x] Play the upgraded instance.
- [x] Confirm upgraded damage is applied.
- [x] Confirm the resolved energy cost is spent.
- [x] Confirm only the upgraded instance enters discard.
- [x] Confirm the normal Strike remains unchanged.
- [x] Confirm the library's base Strike still has base damage.
- [x] Test insufficient energy without state mutation.
- [x] Test an unknown instance ID.
- [x] Test an instance not currently in hand.

## Checkpoint C - Gameplay

- [x] **PASS:** Playing by `instanceId` selects the correct physical copy.
- [x] **PASS:** Gameplay uses upgraded cost, value, and duration.
- [x] **PASS:** A failed play does not spend energy or move the instance.

**Checkpoint C evidence:**

- Test command/result: From `source/` with Microsoft JDK 21.0.8: `.\gradlew.bat --no-daemon spotlessApply spotlessJavaCheck :core:test` — **BUILD SUCCESSFUL**, 855 tests, 0 failures, 0 errors, 0 skipped (234 card tests), verified 15 September 2026. `git diff --check` passes. One new UI test initially needed an EntityService fixture for disposal; corrected and the full suite rerun successfully.
- Evidence: [InstanceCardPlayTest](source/core/src/test/com/csse3200/game/cards/play/InstanceCardPlayTest.java) — 14 focused tests for duplicate-copy identity, upgraded cost/value/duration, failure safety/refunds, actual combat stats, UI events and acquisition.
- Commit/PR: Working-tree changes; not committed or pushed.
- Notes: BattleController now delegates to CardPlayService; both resolve the selected CardInstance through CardResolver. Removed all five base-only/ID-projection BattleDeck bridges. Enemy drop payloads use entity IDs to support exact targeting. Existing BLOCK/status semantics are unchanged. Dynamic artwork/labels/tooltips, save persistence and production shop/reward binding remain outside this checkpoint. See [migration contract](docs/card-play-instance-migration.md).

Do not migrate all 26 cards until the normal/upgraded Strike gameplay flow passes.

---

# Phase 11 - Define the pixel-art asset standard

## Step 31 - Write the shared art specification

- [ ] Set the target widget size to `225 x 456`.
- [ ] Confirm whether source art is `225 x 456` or the recommended `450 x 912` 2x source size.
- [ ] Require PNG artwork without baked-in name, cost, rarity text, description, or upgrade marker.
- [ ] Require a consistent aspect ratio and crop policy.
- [ ] Require snake_case file names matching stable card IDs.
- [ ] Use `Nearest` texture filtering for pixel art.
- [ ] Define a shared palette and visual theme.
- [ ] Define a safe composition area so dynamic labels do not cover important subjects.
- [ ] Check readability at actual battle-hand size.

## Step 32 - Decide source resolution

- [ ] Test native `225 x 456` artwork in game.
- [ ] Test `450 x 912` artwork displayed at `225 x 456` if cards can enlarge on hover.
- [ ] Compare sharpness, memory use, and visual consistency.
- [ ] Record the final source-resolution decision before mass production.

## Step 33 - Create shared UI assets

- [ ] Create or select a reusable card frame.
- [ ] Create a reusable energy-cost badge.
- [ ] Create a reusable upgraded-card overlay or `+` marker.
- [ ] Create rarity colours/icons for common, uncommon, and rare.
- [ ] Create type indicators if required.
- [ ] Ensure frame/layout assets are reused rather than duplicated into every illustration.

## Step 34 - Produce only the representative Strike art

- [ ] Create an art-only pixel version of Strike.
- [ ] Use the agreed file name and dimensions.
- [ ] Keep it free of gameplay text and numbers.
- [ ] Integrate and validate it before producing the other 25 artworks.

**Phase 11 completion evidence:**

- Art specification link:
- Strike art path:
- In-game screenshot:
- Resolution decision:

---

# Phase 12 - Build the reusable `CardWidget`

## Step 35 - Create the widget component

- [ ] Create `components/cards/CardWidget.java`.
- [ ] Make the widget receive a `ResolvedCard` rather than calculating upgrade data itself.
- [ ] Add `setCard(ResolvedCard card)` for initial binding and refresh.
- [ ] Keep gameplay logic out of the widget.
- [ ] Provide an interaction mode if the widget will be reused for battle and read-only library/shop displays.

## Step 36 - Compose the visual layers

- [ ] Add the artwork `Image`.
- [ ] Add the reusable frame.
- [ ] Add the dynamic cost label.
- [ ] Add the dynamic name label.
- [ ] Add rarity presentation.
- [ ] Add type presentation if required.
- [ ] Add an optional dynamic description region if the final face layout includes it.
- [ ] Add the upgraded overlay/marker.
- [ ] Preserve artwork aspect ratio and intended crop.
- [ ] Apply nearest-neighbour filtering.
- [ ] Handle long names without clipping or overflow.

## Step 37 - Support interaction states

- [ ] Define `NORMAL` state.
- [ ] Define `HOVERED` state.
- [ ] Define `DRAGGING` state.
- [ ] Define `UNPLAYABLE` state.
- [ ] Raise or highlight the card on hover.
- [ ] Darken or otherwise identify cards the player cannot afford.
- [ ] Show the upgrade marker only for upgraded instances.
- [ ] Restore the correct position and state after a cancelled drag.
- [ ] Clean up listeners and actors when the widget is removed.

**Phase 12 completion evidence:**

- Widget unit tests:
- Base/upgraded screenshots:
- PR/commit:

---

# Phase 13 - Integrate the real battle hand

## Step 38 - Separate dynamic card-hand UI from generic static buttons

- [ ] Keep `ClickableFactory` responsible for static UI such as Exit and End Turn.
- [ ] Create `components/cards/BattleCardHandDisplay.java`.
- [ ] Give it access to `BattleDeck`, `CardService`, and `CardResolver` through explicit dependencies.
- [ ] Use it as the owner of live card widgets, drag sources, positions, and tooltip interaction.
- [ ] Avoid extending the hard-coded demo-card data model.

## Step 39 - Build the hand from card instances

- [ ] Iterate over `BattleDeck.getHand()` as `CardInstance` values.
- [ ] Retrieve each shared config using `instance.cardId()`.
- [ ] Resolve each instance through `CardResolver`.
- [ ] Build one `CardWidget` per instance.
- [ ] Associate every widget with its `instanceId`.
- [ ] Preserve the current hand ordering and spacing.
- [ ] Handle a missing/invalid definition without crashing the entire UI.

## Step 40 - Update hand-change events

- [ ] Replace `List<String>` hand-change payloads with an instance-aware contract.
- [ ] Prefer a simple `handChanged` notification if the UI can read the current deck itself.
- [ ] Rebuild or diff the hand safely after play, draw, discard, and upgrade.
- [ ] Remove old widgets and input listeners correctly.
- [ ] Hide the tooltip if its card disappears.

## Step 41 - Integrate drag-and-drop

- [ ] Carry `instanceId` in the drag payload.
- [ ] Carry the resolved display name for feedback if required.
- [ ] Send `(instanceId, targetId)` on a valid enemy drop.
- [ ] Send the selected `instanceId` to the self-target flow.
- [ ] Build the drag visual from the same `ResolvedCard`.
- [ ] Hide the tooltip at drag start.
- [ ] Restore the original widget after an invalid/cancelled drop.
- [ ] Unregister drag sources when widgets leave the hand.

**Phase 13 completion evidence:**

- Battle-hand tests:
- Drag/drop test:
- In-game screenshot/video:
- PR/commit:

---

# Phase 14 - Implement hover details

## Step 42 - Create `CardDetailsTooltip`

- [ ] Create `components/cards/CardDetailsTooltip.java`.
- [ ] Make it consume `ResolvedCard` only.
- [ ] Display name.
- [ ] Display cost.
- [ ] Display type.
- [ ] Display rarity.
- [ ] Display target.
- [ ] Display description.
- [ ] Display resolved effect details.
- [ ] Display upgraded state.
- [ ] Reserve an optional area for lore if lore is added later.

## Step 43 - Reuse one tooltip actor

- [ ] Create one tooltip for the whole hand/stage.
- [ ] Update its contents when a different card is hovered.
- [ ] Show it after the agreed hover delay, approximately `0.25-0.4` seconds.
- [ ] Hide it on pointer exit.
- [ ] Hide it at drag start.
- [ ] Hide it when the related widget is removed.
- [ ] Hide it when the screen changes or UI is disposed.
- [ ] Set tooltip touchability to disabled so it does not steal hover.

## Step 44 - Keep the tooltip inside the stage

- [ ] Prefer placing it beside or above the hovered card.
- [ ] Move it to the opposite side near the right/left screen edge.
- [ ] Clamp vertical position to stage boundaries.
- [ ] Check behaviour at different window sizes.
- [ ] Add click-to-inspect or long-press fallback if touch support is required.

## Step 45 - Test hover behaviour

- [ ] Hover normal Strike and confirm base details.
- [ ] Hover upgraded Strike and confirm upgraded details.
- [ ] Confirm cost, rarity, description, value, and duration are resolved values.
- [ ] Confirm the tooltip hides on exit.
- [ ] Confirm the tooltip hides on drag.
- [ ] Confirm the tooltip hides when the card leaves the hand.
- [ ] Confirm the tooltip does not leave the visible stage.
- [ ] Confirm moving between cards refreshes content without stale data.

## Checkpoint D - UI/data consistency

- [ ] **PASS:** `CardWidget`, tooltip, and gameplay show/use the same resolved values.
- [ ] **PASS:** A normal and upgraded copy of the same card display differently while sharing artwork.
- [ ] **PASS:** Hover and drag interactions do not conflict.

**Checkpoint D evidence:**

- UI test result:
- Screenshot/video:
- Gameplay comparison:
- Commit/PR:

---

# Phase 15 - Replace demo/library and prepare shop displays

## Step 46 - Replace hard-coded `CardHandDisplay` data

- [ ] Remove or retire the `DEMO_HAND` list after the real reusable widget is ready.
- [ ] Load real card data through `CardService`.
- [ ] Render cards using `CardWidget` in read-only mode.
- [ ] Disable battle drag behaviour in the library view.
- [ ] Reuse `CardDetailsTooltip`.
- [ ] Decide whether the library shows base, upgraded, or both views.
- [ ] Rename components if needed so battle hand and card library responsibilities are clear.

## Step 47 - Prepare shop/reward presentation

- [ ] Allow `CardWidget` to render in display-only/shop mode.
- [ ] Keep offers keyed by `cardId` until purchased.
- [ ] Create a new instance only when the player acquires the card.
- [ ] Show the correct base/upgraded preview according to the shop design.
- [ ] Reuse dynamic labels and tooltip instead of duplicating display logic.
- [ ] Record any incomplete production integration honestly.

**Phase 15 completion evidence:**

- Library screenshot:
- Shop/reward screenshot or dependency record:
- PR/commit:

---

# Phase 16 - Add upgrade data for the full library

## Step 48 - Upgrade every official card definition

Add and verify upgrade data for each card as it exists in the official library.

- [ ] Strike
- [ ] Defend
- [ ] Poison Dagger
- [ ] Expose
- [ ] Inner Focus
- [ ] Bandage
- [ ] Starfall
- [ ] Rift Lance
- [ ] Astral Ward
- [ ] Resurrection
- [ ] Remaining card 11
- [ ] Remaining card 12
- [ ] Remaining card 13
- [ ] Remaining card 14
- [ ] Remaining card 15
- [ ] Remaining card 16
- [ ] Remaining card 17
- [ ] Remaining card 18
- [ ] Remaining card 19
- [ ] Remaining card 20
- [ ] Remaining card 21
- [ ] Remaining card 22
- [ ] Remaining card 23
- [ ] Remaining card 24
- [ ] Remaining card 25
- [ ] Remaining card 26

Replace placeholder names with final card IDs when the other definitions are merged.

## Step 49 - Review every base/upgrade pair

For every official card:

- [ ] The base definition validates.
- [ ] The upgrade definition validates.
- [ ] The upgraded name is correct.
- [ ] The upgraded cost is correct.
- [ ] The upgraded rarity is correct.
- [ ] The upgraded description matches gameplay.
- [ ] Upgrade effects are in the intended execution order.
- [ ] Upgrade values are correct.
- [ ] Upgrade durations are correct.
- [ ] Effects remain compatible with the inherited target.
- [ ] Base and upgrade resolve to the same artwork path.
- [ ] The upgrade has a documented balance purpose.

## Step 50 - Control description consistency

- [ ] For this sprint, keep explicit base and upgrade descriptions in JSON if required by the agreed schema.
- [ ] Review every description against its resolved effect values and durations.
- [ ] Decide whether a later `CardRulesTextFormatter` should generate standard rules text automatically.
- [ ] If a formatter is implemented, support every existing effect before making it authoritative.
- [ ] Keep optional lore separate from rules text.

**Phase 16 completion evidence:**

- Official card count:
- Config validation result:
- Balance review link:
- PR/commit:

---

# Phase 17 - Replace all card artwork

## Step 51 - Lock the layout before mass production

- [ ] Confirm Strike is readable at `225 x 456`.
- [ ] Confirm the name does not overflow.
- [ ] Confirm cost is immediately readable.
- [ ] Confirm rarity and upgrade state are distinguishable.
- [ ] Confirm hover and tooltip work.
- [ ] Confirm drag preview works.
- [ ] Confirm artwork is not stretched or unexpectedly cropped.
- [ ] Freeze the frame, safe areas, and pixel-art specification.

## Checkpoint E - Representative asset

- [ ] **PASS:** One art-only pixel Strike works through config, resolver, UI, hover, drag, and gameplay at the real hand size.

**Checkpoint E evidence:**

- Screenshot/video:
- Art review:
- UI review:

Do not produce the remaining 25 final artworks until Checkpoint E passes.

## Step 52 - Produce the 26 pixel artworks

For every artwork:

- [ ] Use the approved dimensions and aspect ratio.
- [ ] Use the approved pixel-art palette/style.
- [ ] Keep text, numbers, rarity, description, and upgrade markers out of the PNG.
- [ ] Keep important subjects outside dynamic-label safe areas.
- [ ] Use a snake_case file name matching the card ID.
- [ ] Verify the image at actual hand size.
- [ ] Verify base and upgraded instances can reuse it.
- [ ] Record author/source/licensing information where required.

## Step 53 - Complete the asset migration safely

- [ ] Update all card texture paths as needed.
- [ ] Load card textures through the project's resource-management path rather than unmanaged per-card textures.
- [ ] Avoid a static texture cache with no clear disposal lifecycle.
- [ ] Check GPU-memory impact at the chosen source resolution.
- [ ] Confirm no production config/code references old full-card images.
- [ ] Confirm all new textures load before removing old assets.
- [ ] Remove superseded assets only after verification and with Git history available for recovery.

**Phase 17 completion evidence:**

- Asset inventory:
- Missing-path check:
- Memory/performance notes:
- In-game screenshots:
- PR/commit:

---

# Phase 18 - Complete automated and manual verification

## Step 54 - Run model and configuration unit tests

- [ ] `CardUpgradeConfig` tests pass.
- [ ] `CardInstance` tests pass.
- [ ] `CardInstanceFactory` tests pass.
- [ ] `CardResolver` tests pass.
- [ ] `CardConfigLoader` tests pass.
- [ ] `CardValidator` tests pass.
- [ ] `PlayerDeck` tests pass.
- [ ] `BattleDeck` tests pass.

## Step 55 - Run gameplay integration tests

- [ ] Base damage resolves correctly.
- [ ] Upgraded damage resolves correctly.
- [ ] Base cost resolves correctly.
- [ ] Upgraded cost resolves correctly.
- [ ] Base duration resolves correctly.
- [ ] Upgraded duration resolves correctly.
- [ ] The correct instance is played.
- [ ] The correct instance is discarded.
- [ ] Failure spends no energy.
- [ ] Failure moves no card.
- [ ] Upgrade state survives draw/discard/reshuffle.
- [ ] Two copies with the same card ID can have different upgrade levels.

## Step 56 - Run UI tests

- [ ] Widget labels come from `ResolvedCard`.
- [ ] The upgrade marker appears only for upgraded instances.
- [ ] The shared artwork displays correctly.
- [ ] Hover shows the correct tooltip.
- [ ] Tooltip hides correctly.
- [ ] Unaffordable cards show the agreed disabled state.
- [ ] Hand rebuilding removes obsolete widgets and listeners.
- [ ] Removed card widgets unregister their drag sources.
- [ ] UI tests verify normal and upgraded copies together.

## Step 57 - Run regression tests

- [ ] Run focused card tests:

```powershell
.\gradlew.bat core:test --tests "com.csse3200.game.cards.*"
```

- [ ] Run the complete core test suite:

```powershell
.\gradlew.bat core:test
```

- [ ] Record failures, owners, fixes, and rerun evidence.
- [ ] Confirm the six original cards still load and work.
- [ ] Confirm existing battle flow still starts and completes.

## Step 58 - Perform manual in-game verification

- [ ] Start a battle with multiple copies of Strike.
- [ ] Upgrade only one Strike instance.
- [ ] Hover each Strike and compare details.
- [ ] Confirm only the upgraded copy shows upgraded presentation.
- [ ] Drag the upgraded Strike to a valid enemy.
- [ ] Confirm the correct damage is applied.
- [ ] Confirm the correct energy is spent.
- [ ] Confirm the correct instance enters discard.
- [ ] End turns until discard reshuffles.
- [ ] Draw the upgraded Strike again.
- [ ] Confirm it remains upgraded.
- [ ] Test an invalid target.
- [ ] Test insufficient energy.
- [ ] Check five-card hand spacing and overlap.
- [ ] Check long names and descriptions.
- [ ] Resize the window and verify layout/tooltip bounds.
- [ ] Check artwork sharpness and cropping.

## Checkpoint F - Full feature

- [ ] **PASS:** All 26 official cards have valid base and upgrade data.
- [ ] **PASS:** All 26 cards have valid pixel-art assets.
- [ ] **PASS:** Per-instance upgrades survive the complete deck lifecycle.
- [ ] **PASS:** UI, tooltip, and gameplay use identical resolved values.
- [ ] **PASS:** Focused and full regression suites pass.
- [ ] **PASS:** The integrated game demonstrates normal and upgraded copies of the same card.

**Checkpoint F evidence:**

- Full test output:
- Demo recording/screenshots:
- Final card inventory:
- Remaining known issues:

---

# Phase 19 - Documentation and delivery evidence

## Step 59 - Document the data model

- [ ] Document `CardConfig` as the shared base definition.
- [ ] Document `CardUpgradeConfig` as the upgraded variant data.
- [ ] Document `CardInstance` as one owned runtime copy.
- [ ] Document `ResolvedCard` as the immutable values used by UI and gameplay.
- [ ] Add JSON examples for base and upgrade data.
- [ ] Add a diagram showing lookup and resolution.

## Step 60 - Document the integration contract

- [ ] Document that card lookup uses `cardId`.
- [ ] Document that play, discard, removal, and upgrade use `instanceId`.
- [ ] Document how shops/rewards create a new instance.
- [ ] Document how upgrade state persists through deck piles.
- [ ] Document that UI must not mutate `CardConfig` or `CardInstance` directly.
- [ ] Document `CardResolver` as the source of final values.
- [ ] Document failure behaviour for unknown instance, missing config, missing upgrade, invalid target, and insufficient energy.
- [ ] Link relevant issues and PRs.

## Step 61 - Collect final evidence

- [ ] Attach model/configuration test results.
- [ ] Attach deck/gameplay integration test results.
- [ ] Attach full regression results.
- [ ] Attach screenshots of normal and upgraded copies together.
- [ ] Attach tooltip screenshots.
- [ ] Attach a gameplay demonstration of an upgraded card.
- [ ] Attach the final JSON example.
- [ ] Attach the final class/data-flow diagram.
- [ ] Attach the pixel-art specification and inventory.
- [ ] Record balance decisions and revisions.
- [ ] Record any incomplete external integration honestly.
- [ ] Update the main Sprint 2 task checklist and ticket.

**Phase 19 completion evidence:**

- Documentation links:
- Final PR/commit:
- Demo link:
- Submission notes:

---

# Recommended commit sequence

- [ ] 1. `Add card upgrade configuration model`
- [ ] 2. `Add runtime card instance model`
- [ ] 3. `Add resolved card model and resolver`
- [ ] 4. `Migrate player deck to card instances`
- [ ] 5. `Migrate battle deck to card instances`
- [ ] 6. `Use instance IDs in card play requests`
- [ ] 7. `Resolve upgraded cards during gameplay`
- [ ] 8. `Add dynamic Scene2D card widget`
- [ ] 9. `Add card details tooltip`
- [ ] 10. `Integrate dynamic battle hand`
- [ ] 11. `Add shared card UI and pixel-art assets`
- [ ] 12. `Add upgrades for all card definitions`
- [ ] 13. `Update library and shop card displays`
- [ ] 14. `Add end-to-end verification and documentation`

Each commit should compile and should include the tests relevant to its change. Avoid combining the entire schema, deck, gameplay, UI, and asset migration into one commit.

# Progress dashboard

| Phase | Description | Status | Evidence/notes |
| --- | --- | --- | --- |
| 1 | Integration contract | In progress | Decisions are documented; team notifications and real issue/PR links remain pending. |
| 2 | Configuration schema | Complete | `CardUpgradeConfig`, optional `CardConfig.upgrade`, and the Strike+ vertical slice are committed. |
| 3 | Loading and validation | Complete | Nested upgrade fields, compatibility rules, error paths, loader fixtures and regression tests pass. |
| 4 | Runtime card identity | Complete (uncommitted) | Immutable `CardInstance`, UUID factory and 11 runtime-identity tests pass. |
| 5 | Card resolution | Complete (uncommitted) | Base/upgraded Strike resolution and defensive effect copies pass all tests. |
| 6 | PlayerDeck migration | Complete (uncommitted) | Per-copy add/remove/upgrade, snapshots, copy isolation and compatibility audit tested; see Checkpoint B evidence. |
| 7 | BattleDeck migration | Complete (uncommitted) | Instance piles preserve identity; the temporary bridges used at B have been removed at C. |
| 8 | Starter deck and acquisition | Starter/contract complete; external wiring pending | Explicit instance factory and repeated acquisition contract tested; production inventory-backed shop/rewards remain an external dependency. |
| 9 | Card-play request migration | Complete (uncommitted) | Unified cards.play request/result; instanceId UI payloads, entity-ID targets and instance snapshots. |
| 10 | Gameplay resolution | Complete (uncommitted) | Shared CardPlayService resolves selected cost/effects for both service and live controller; 14 new focused tests and full regression pass. |
| 11 | Pixel-art specification | Not started | |
| 12 | Dynamic CardWidget | Not started | |
| 13 | Battle-hand integration | Not started | |
| 14 | Hover tooltip | Not started | |
| 15 | Library/shop display | Not started | |
| 16 | Full upgrade configurations | Not started | |
| 17 | Full artwork migration | Not started | |
| 18 | Verification | Not started | |
| 19 | Documentation and evidence | Not started | |

# Checkpoint dashboard

| Checkpoint | Required outcome | Status | Evidence |
| --- | --- | --- | --- |
| A - Data | Strike base/upgrade load, validate, and resolve without mutation | **PASS** | 199 card tests and Spotless passed on 11 September 2026. |
| B - Deck | Unique instance identity survives the deck lifecycle | **PASS** | 841 core tests (220 card tests), Spotless and diff check passed on 15 September 2026. |
| C - Gameplay | Correct instance uses upgraded cost/effects | **PASS** | 855 core tests (234 card tests), Spotless and diff check passed on 15 September 2026. |
| D - UI | Widget, tooltip, and gameplay show/use the same values | Not started | |
| E - Asset | Pixel-art Strike works at real hand size | Not started | |
| F - Full feature | All 26 cards, assets, tests, integration, and evidence complete | Not started | |

# Blocker and decision log

Add a row whenever a decision changes or progress is blocked.

| Date | Area | Decision/blocker | Owner | Due date | Resolution/evidence |
| --- | --- | --- | --- | --- | --- |
| 2026-09-10 | Runtime identity | Per-copy upgrades require stable `instanceId` values | Raymond | | Confirmed in planning |
| 2026-09-10 | Artwork | Base and upgrade reuse one pixel artwork and shared layout | Raymond | | Confirmed in planning |
| 2026-09-11 | Upgrade integration | Continue with `CardConfig.upgrade` and `CardInstance`; do not adopt separate `_upgraded` card IDs | Raymond / upgrade team | | Checkpoint A proves the instance-based data contract; cross-team confirmation remains pending |
| 2026-09-15 | Checkpoint B boundary | Use strict instance deck APIs with explicit base-only bridges for existing gameplay until C; upgraded cards are not yet shown by the legacy BattleScreen | Raymond | Checkpoints C/D | [Contract and removal gates](docs/card-deck-identity.md); both play paths have rejection regression tests |
| 2026-09-15 | Persistence / live upgrades | ID-only saves do not preserve instance identity or upgrades; player-deck upgrades do not change an already-created battle snapshot | Raymond / dependent owners | Before integrated upgrade release | Runtime pile preservation is verified; save schema and live-upgrade synchronization are not implemented here |
| 2026-09-15 | Checkpoint C API | Consolidate request/result under cards.play; remove duplicate classes and base-only BattleDeck bridges; enemy drops use per-entity IDs | Raymond / dependent teams | Before merge | [Breaking migration guide](docs/card-play-instance-migration.md); tests pass, team sign-off pending |
| 2026-09-15 | Acquisition scope | Starter decks and fresh-instance acquisition contract complete; current inventory-backed shop/reward wiring remains external | Raymond / acquisition owners | Before integrated release | Factory and acquisition-contract tests pass; production purchase integration not claimed |
| | | | | | |

# Final definition of done

This task is complete only when all of the following are true:

- [ ] Every owned card copy has a stable, unique `instanceId`.
- [ ] One copy can be upgraded without changing other copies with the same `cardId`.
- [ ] Every official card has valid base and upgrade configuration.
- [ ] Card definitions remain read-only during runtime resolution.
- [ ] Deck, hand, draw, discard, reshuffle, play, and upgrade operations preserve instance identity.
- [ ] Card play uses resolved cost, effect values, and durations.
- [ ] Card UI contains no gameplay text baked into artwork.
- [ ] CardWidget dynamically renders the agreed fields.
- [ ] Base and upgraded cards reuse the same artwork and layout.
- [ ] Tooltip displays complete resolved information and behaves correctly with hover/drag.
- [ ] All 26 pixel-art assets follow the agreed specification and load correctly.
- [ ] Automated card and full core tests pass.
- [ ] Manual integration checks pass at the actual in-game hand size.
- [ ] Cross-team API changes and remaining dependencies are documented.
- [ ] Screenshots, test logs, PRs, and demonstration evidence are attached.

# Battle card targeting

Battle cards use their configured `TargetType` to choose their drag interaction:

| Target type | How to play | Preview |
| --- | --- | --- |
| `SINGLE_ENEMY` | Drag toward a living enemy and release in the battlefield. The pointer snaps to a nearby enemy. | A curved, tapered arrow turns red when a target is selected. A thin line below each target turns red and thicker when selected. |
| `SELF` | Drag the card out of the hand and release anywhere in the battlefield. | The player is highlighted on a valid drop. |
| `ALL_ENEMIES` | Drag the card out of the hand and release anywhere in the battlefield. | All living enemies are highlighted on a valid drop. |

The battlefield is the central play area above the raised hand and below the top controls and battle log. Its bounds are supplied by `BattleScreen` in stage coordinates and include clearance for the card hover animation. Releasing in the hand, over the top controls, or outside the stage cancels the play. No energy is spent on cancellation. Normal battle validation still rejects unaffordable cards, unavailable targets, and plays outside the player's turn.

## Automatic turn ending

When the player runs out of energy, the turn ends automatically after one second of gameplay time. The last card resolves before the countdown starts. Recovering energy cancels the countdown; spending it all again starts a fresh delay. Pausing the game pauses the delay, and the End Turn button still works immediately.

The countdown resets when the battle changes phase and waits for the enemy turn presentation to finish before starting on a new player turn.

## Implementation

- `CardAimController` manages the preview and resolves a valid release for each target type. It uses live entity positions and ignores defeated targets.
- `AimArrowActor` draws the curved arrow, target underlines, and battlefield corner markers. It does not intercept input.
- `DragNDrop` dispatches one `playCard(instanceId, targetId)` event on a valid release. Single-enemy cards stay in the hand during aiming; self and all-enemy cards follow the pointer.
- `Team3CardPlayAdapter` converts the event to the card's configured target type. The `allEnemies` UI marker does not select an individual enemy.
- The existing card play service and battle controller validate costs and apply effects.
- `BattleActions` checks energy through `BattleController` and updates the one-second countdown using the shared, pause-aware `GameTime`.

## Verification

Run `./gradlew.bat :core:test` with JDK 21. `CardAimControllerTest` covers empty battlefield drops, cancellation, single-enemy selection, unavailable targets, energy costs, self effects, and effects on multiple enemies. Existing drag and target-selector tests cover the original interaction. `BattleActionsTest` covers the automatic turn delay, pausing, energy recovery, manual turn ending, enemy presentation, and cards spending or restoring the last energy.

For a gameplay check, drag an attack between enemies, drag a self card into empty battlefield space, and drag an all-enemy card into empty battlefield space. Return a card to the hand to confirm cancellation, and try an unaffordable card to confirm that it stays in hand without applying effects.

To check the automatic turn ending in gameplay, spend the last energy and confirm the enemy turn begins after one second. Repeat while pausing during the delay, then check that an energy-restoring card keeps the turn open.

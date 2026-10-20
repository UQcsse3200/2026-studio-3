# Battle card targeting

Battle cards use their configured `TargetType` to choose their drag interaction:

| Target type | How to play | Preview |
| --- | --- | --- |
| `SINGLE_ENEMY` | Drag toward a living enemy and release in the battlefield. The pointer snaps to a nearby enemy. | A curved, tapered arrow turns red when a target is selected. A thin line below each target turns red and thicker when selected. |
| `SELF` | Drag the card out of the hand and release anywhere in the battlefield. | The player is highlighted on a valid drop. |
| `ALL_ENEMIES` | Drag the card out of the hand and release anywhere in the battlefield. | All living enemies are highlighted on a valid drop. |

The battlefield is the central play area above the raised hand and below the top controls and battle log. Its bounds are supplied by `BattleScreen` in stage coordinates and include clearance for the card hover animation. Releasing in the hand, over the top controls, or outside the stage cancels the play. No energy is spent on cancellation. Normal battle validation still rejects unaffordable cards, unavailable targets, and plays outside the player's turn.

## Turn timing

When the player runs out of energy, their turn ends on the next update after the last card resolves. The enemy then takes its turn. After the enemy action and effects are shown, there is a three-second pause before the player's hand returns and the next player turn is announced. The pause also applies when the player uses End Turn manually.

Pausing gameplay pauses the three-second wait. The hand stays down throughout the enemy turn and the following pause, and repeated End Turn presses are ignored until control returns. Cards that restore energy during resolution keep the player's turn open.

## Implementation

- `CardAimController` manages the preview and resolves a valid release for each target type. It uses live entity positions and ignores defeated targets.
- `AimArrowActor` draws the curved arrow, target underlines, and battlefield corner markers. It does not intercept input.
- `DragNDrop` dispatches one `playCard(instanceId, targetId)` event on a valid release. Single-enemy cards stay in the hand during aiming; self and all-enemy cards follow the pointer.
- `Team3CardPlayAdapter` converts the event to the card's configured target type. The `allEnemies` UI marker does not select an individual enemy.
- The existing card play service and battle controller validate costs and apply effects.
- `BattleActions` ends an empty player turn after card resolution. It reveals enemy effects, then uses the shared, pause-aware `GameTime` to delay returning the hand for three seconds. `CardActions` follows the presented phases to show and hide the hand.

## Verification

Run `./gradlew.bat :core:test` with JDK 21. `CardAimControllerTest` covers empty battlefield drops, cancellation, single-enemy selection, unavailable targets, energy costs, self effects, and effects on multiple enemies. Existing drag and target-selector tests cover the original interaction. `BattleActionsTest` covers immediate energy-based turn ending, the pause after enemy effects, pausing gameplay, repeated End Turn presses, disposal, hand visibility, and cards spending or restoring the last energy.

For a gameplay check, drag an attack between enemies, drag a self card into empty battlefield space, and drag an all-enemy card into empty battlefield space. Return a card to the hand to confirm cancellation, and try an unaffordable card to confirm that it stays in hand without applying effects.

To check the timing in gameplay, spend the last energy and confirm the enemy turn begins immediately. After the enemy action is shown, the hand should stay down for three seconds before your turn returns. Repeat with the End Turn button and while pausing during the wait, then check that an energy-restoring card keeps the current turn open.

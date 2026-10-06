# Battle tutorial integration contract

Base: `Feature-2-Sprint3-TutorialTooltips`, commit `50dad1a3`.

This module observes an isolated teaching battle. It does not start that battle,
change its AI or rules, create a normal run, or grant rewards. It is not installed
in the normal `BattleScreen` or menu yet.

## Steps and UI

`BattleTutorialView` is the UI boundary for Jun:

- `bindActions(Runnable continueAction, Runnable exitAction)` wires Continue and Exit Tutorial.
- `show(BattleTutorialPrompt prompt)` replaces the current instruction and highlight.
- `clear()` removes hints, highlights and handlers. It must be idempotent and must
  not dispose shared battle actors, skins or the game's Stage.

The prompt contains `step`, `text`, `highlight` and `canContinue`. Highlight targets
are semantic keys, not pixel coordinates. Resolve them against the live hand,
card cost, energy, health, status effects, draw pile, End Turn button and enemies.
When a target is not visible (for example, no active status effect), still show
the text. Do not block the lesson or obscure playable cards/buttons. Recompute
bounds after resizing or rebuilding the hand.

| Step | Shared content | Highlight | Advancement |
| --- | --- | --- | --- |
| HAND | OPENING_HAND | HAND | Continue |
| CARD_COST | CARD_COST | CARD_COST | Continue |
| ENERGY | ENERGY | ENERGY | Continue |
| HEALTH | HEALTH | HEALTH | Continue |
| BUFFS | BUFFS_AND_STATUS | STATUS_EFFECTS | Continue |
| CARD_DRAW | CARD_DRAW | DRAW_PILE | Continue |
| BATTLE_OUTCOME_RULES | WIN_OR_LOSE | ENEMIES | Continue |
| PLAY_A_CARD | PLAY_A_CARD | HAND | Successful card play |
| END_TURN | END_TURN | END_TURN | Actual PLAYER_END phase |
| FREE_PLAY | WIN_OR_LOSE | NONE | Real battle end or Exit Tutorial |

All eight concept categories from Yihan's `BattleTutorialPromptContent` are used
directly. The separate `PLAY_A_CARD` action prompt says "Now play an affordable
card from your hand." rather than repeating the cost explanation. There is no
second copy of the rule text. Rejected card plays do not advance the
lesson. Earlier successful actions count, but End Turn only counts after a
successful play. A battle ending during explanations still reports its outcome
once. Information prompts do not pause the battle or impose new input rules.

Integration update after Yihan's review: `Step.DRAW_AND_TURNS` is now
`Step.CARD_DRAW`. Update any UI step switches or mappings to the new key. It
still highlights `DRAW_PILE` and advances with Continue; the separate `END_TURN`
action step and its real-battle-event requirement are unchanged. No view method,
highlight key, step order, or completion callback has changed.

## Tutorial battle owner (Guoqing)

Provide an isolated tutorial screen/battle with:

- the actual `BattleController` before combat starts;
- one existing ordinary enemy with Team 1's tutorial-specific safe behaviour
  integrated into the encounter; Team 1 owns that enemy behaviour, while
  Guoqing owns the tutorial battle setup and integration;
- an appropriate starter hand (five playable cards for the intended
  demonstration, including affordable examples);
- no normal map-node completion, victory rewards, autosave or regular end-screen
  transition from this tutorial battle;
- registration of the guidance component in the screen's entity lifecycle, so
  its `update()` and `dispose()` run normally.

Construct and register the component before starting combat:

```java
BattleTutorialComponent guidance =
    new BattleTutorialComponent(
        battleController,
        tutorialView,
        outcome -> game.startNewRun());
Entity guideEntity = new Entity().addComponent(guidance);
ServiceLocator.getEntityService().register(guideEntity);
// The tutorial battle owner now starts its battle, using its own agreed API.
```

Use the normal entity registration path; do not also call `guidance.create()`
manually. `Outcome` is `WON`, `LOST`, or `CANCELLED`. The owner chooses whether to
show a result/retry screen before continuing; the callback above illustrates
continuing to a fresh run for every outcome. This policy must be agreed with the
flow owner before final wiring.

The component queues completion from `update()` via `Gdx.app.postRunnable`, clears
its UI and removes its three battle observers before calling the owner. Do not
also attach the ordinary `BattleActions` victory/defeat navigation unchanged.
Screen disposal is silent and cancels queued tutorial navigation: simply leaving
a screen must not unexpectedly start a new run.

## Menu and fresh run owner (Joel)

- Subscribe to `MainMenuDisplay.ENTER_TUTORIAL_EVENT` and invoke Guoqing's agreed
  tutorial launch method. This method is still to be supplied.
- Skip Tutorial already follows `game.startNewRun()`.
- Tutorial completion uses that same method, not `setScreen(MAP)` directly.
  It disposes the current screen before resetting run state, plays the opening
  story, and then enters the new map.

## Lifecycle and validation

`BattleTutorialController.close()` is idempotent and does not report cancellation.
Call it outside battle event dispatch; `BattleTutorialComponent` handles that
timing. User-requested Exit Tutorial calls `cancel()` instead. Do not use
`cancel()` inside a screen's `dispose()`.

The only shared battle changes are additive removal methods for the existing
card-play, phase-change and battle-end listeners. Other observers remain intact.

Focused automated check:

```powershell
.\gradlew.bat :core:test --tests 'com.csse3200.game.tutorial.*' --tests 'com.csse3200.game.components.combat.BattleControllerTest' --offline --console plain --max-workers=2
```

After the real screen/UI are connected, manually verify entry, skip, Continue,
invalid/valid plays, End Turn, early victory/defeat, Exit Tutorial, other screen
exits, and a second tutorial visit. Confirm a fresh normal run has no tutorial
health loss, cards, gold, rewards or hints, and that opening-story skip works.
The tutorial does not claim that five new cards are drawn automatically every
turn; drawing and cooldowns must match the actual combat implementation.

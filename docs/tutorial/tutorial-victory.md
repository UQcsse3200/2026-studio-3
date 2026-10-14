# Tutorial Victory Screen

Related task: [#448](https://github.com/UQcsse3200/2026-studio-3/issues/448), scope 3.

After a real tutorial battle victory, `BattleActions` starts the same `EnemyReleaseComponent` / `EnemyReleaseDisplay` sequence used in normal battles. It waits for every available release animation to finish before opening `TutorialVictoryScreen`. The existing release lasts two seconds and includes the upright pose, warm tint, light beam and rising particles.

The tutorial completion page reuses the dungeon background, victory title, reward-panel frame and pixel button style. It displays a short completion message and one Continue button, without generating rewards or completing a normal map encounter.

Continue is processed once after the UI finishes updating and rendering. It calls the existing `GdxGame.startNewRun()` path, which disposes the outgoing screen, resets the run and plays the opening narration before the fresh Map. Tutorial state and rewards do not carry into that run, and the Map introduction remains available.

Tutorial defeat returns to the main menu. Voluntary Exit still starts a fresh normal run directly. Normal battle victory continues to use the existing reward screen and encounter autosave.

Automated coverage:

- Tutorial victory waits for all release animations and does not modify or autosave the normal run.
- Normal victory retains its animation wait, encounter completion and reward-screen destination.
- Tutorial defeat does not play release animations.
- Disposing a battle cancels queued result navigation.
- Continue waits for input, starts once outside input dispatch and uses the existing reset/narration/Map flow.
- Disposing the completion screen cancels pending Continue and unloads its assets once.
- The completion panel remains inside the viewport after resizing.

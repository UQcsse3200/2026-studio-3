# Map Introduction

Related task: [#448](https://github.com/UQcsse3200/2026-studio-3/issues/448), scope 1.

The first Map of each fresh run opens a **Your Journey** panel in the left margin. It explains reaching the Boss, selecting the first available node connected to the starting point, completing encounters, and choosing one route at each fork. Node types remain covered by the existing Map legend.

**Got it** collapses the panel into a **?** button, which reopens the same explanation. The panel has no modal overlay and does not take keyboard or scroll focus. Nodes outside the panel remain interactive.

The panel reuses the Inventory background (`images/ui/inventory-panel.png`) as a nine-patch so its corners retain their proportions in the narrow sidebar. Its gold heading and cream body text match the Inventory palette. Both **?** and **Got it** use `BattleMenuSkins.frameStyle(...)`, the shared bronze frame introduced by Maria in [PR #487](https://github.com/UQcsse3200/2026-studio-3/pull/487). The group branch includes that small style API from `main`; no JSON button migration is required.

`RunState.startRun()` marks the introduction pending. `MapIntroduction` consumes this flag once when the Map display is created. Returning from encounters does not show the panel again. Restoring a saved run starts with the panel collapsed, including saves at the starting point. Ending the run clears the pending flag; starting another fresh run makes the introduction available again. The save format is unchanged.

`MapDisplay` owns the introduction, repositions it below the stats bar on resize, and removes it on disposal. Fonts are borrowed from the shared Map skin. The panel texture is managed with the Map assets, and the button frames are shared through `BattleMenuSkins`.

## Verification

- On 18 October 2026, the UI consistency update passed all 31 focused Map, Inventory, menu-layout and shared-button tests, plus formatting and desktop compilation. Fresh-run, collapse/reopen, return/restore, hover and resize were rendered for visual checks at 1280×800 and 1600×900.

- Five `MapIntroductionTest` cases cover collapse/reopen, returning Map display, unobstructed node selection, layout after resize, and control cleanup.
- Four added `RunStateTest` cases cover first display per fresh run, encounter return, restoration at the start, and clearing an unshown introduction.
- Focused regression also covers `MapSelectionControllerTest`, `MapProgressionEndToEndTest`, `SaveGameRestoreServiceTest`, and `GdxGameTest`.
- Recorded on 13 October 2026: all 73 focused tests passed; `formatCheck`, desktop compilation, and `git diff --check` passed.
- A hidden LWJGL desktop render verified fresh-run display, collapse, reopen, Map recreation, restored-run display, and another fresh run. Layout was visually checked at 1280×800 and 1600×900. Screenshots are retained in the task workspace under `output/sprint4-map-introduction`.

On Windows with the established JDK 21 and `C:/gradle-cache`, use:

```bash
./gradlew -g C:/gradle-cache -Dfile.encoding=GBK core:test \
  --tests 'com.csse3200.game.maps.MapIntroductionTest' \
  --tests 'com.csse3200.game.maps.RunStateTest' \
  --tests 'com.csse3200.game.maps.MapSelectionControllerTest' \
  --tests 'com.csse3200.game.maps.MapProgressionEndToEndTest' \
  --tests 'com.csse3200.game.save.SaveGameRestoreServiceTest' \
  --tests 'com.csse3200.game.GdxGameTest'
./gradlew -g C:/gradle-cache formatCheck desktop:compileJava
```

The encoding option matches the Windows launcher when its argument files contain Chinese paths. It is a local test invocation setting, not a project configuration change.

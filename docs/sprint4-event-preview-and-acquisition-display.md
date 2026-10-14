# Sprint 4 Event preview and acquisition display (#439)

## Event preview for UI review

From the main menu, select **Demo Event**, then choose any configured Event or **Random Event**.
The menu reads the Event catalogue when opened, so newly configured Events appear without adding
another hard-coded shortcut. **Cancel** returns to the menu. A selected preview starts an isolated
`DemoEventScreen`; its completion returns to the menu. Normal map Event selection is unchanged.

Use the picker to inspect the Dice Game, Wishing Fountain, Wandering Healer, Mysterious Shrine,
Abandoned Mine, Flooded Crossing and Card Fusion screens without relying on a random roll. It
remains a development shortcut and is not evidence that the Map-to-Event flow has been tested.

## Confirmed result display

`AcquisitionResultDisplay` implements Zixuan's `AcquisitionView` interface. It displays a mixed
list of cards, items, positive changes and negative changes in one scrollable panel. Cards use
the shared `CardWidget` preview when its definition and managed artwork are available; the
confirmed result name and description remain visible when artwork is unavailable. Quantity and
explicit duration are displayed separately. The display contains no inventory, currency, deck,
effect or map mutation code.

- Shop purchases use the `ShopPurchaseReceipt.acquisition()` prepared after the charge and card
  grant. Failed purchases do not open a result panel. Dismissing the panel resumes the Shop; it
  does not issue another purchase. Shop exit and component disposal cancel pending callbacks.
- Integrated Chance encounters build read-only result entries only after `ChanceResolution`
  reports `APPLIED`. Health and gold entries use the **actual before/after values**, including
  capped healing, and card entries use the confirmed reward IDs. No-change outcomes keep the
  existing Outcome and Continue flow. **Continue** completes the resolved encounter once;
  **Close** returns to its Outcome and exposes the original Continue button. Disposal cannot
  complete the encounter.
- The encounter-scoped `AcquisitionPresentation` rejects duplicate result IDs, concurrent panels
  and stale callbacks. The underlying Shop/Chance services remain responsible for applying
  rewards. The UI never calls them on dismissal.

This integration variant includes Yihan's Event item branch (`724a5a0`). Only an `APPLIED`
resolution contributes item IDs to the panel; failed transactions, including an unregistered
Curse Item, display an error and no acquisition panel. The selected configured choice supplies
the player's item-effect explanation, while the committed outcome supplies the item IDs and
quantities. The text does not grant or activate an item. Shrine and Mine scenes show four choices
in two rows at 1280 × 800; the existing two-choice layouts remain unchanged.

Team 7's Curse Item definitions are still a dependency on this temporary branch. A full
Map-to-Event-to-Map Curse playthrough and narrow-window layout require manual acceptance after
those definitions are present. Do not claim that these paths passed because the temporary branch
compiles or because the independent result-panel tests pass.

## Verification checklist

Focused regressions cover picker catalogue coverage, specific and random selection, cancellation,
mixed result rendering/dismissal, the Shop's one-charge purchase flow, a committed Chance
result followed by Close and Continue, four-choice scene positioning, and display of an item
only after its transaction commits. For desktop acceptance, open each named Event via the
picker at normal and narrow window sizes; inspect title, choice and outcome readability; verify
that a successful Shop purchase displays its card exactly once; and then verify the normal
Map -> Event/Shop -> Map journey after the integrated branches are reviewed. Record actual results
and screenshots in the feature issue or Wiki. A local Gradle test run is required on Java 21;
the current Codex container could not download Gradle 8.5 from `services.gradle.org`.

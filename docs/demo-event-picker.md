# Demo Event picker

The main-menu Demo Event button opens a catalogue chooser with Random Event and Back.
Event names are derived from `configs/chanceEncounters.json`, so new configured events appear automatically.
The chosen ID is passed once to `GdxGame.openDemoEvent(String)` and the existing isolated
`DemoEventScreen`. Nothing is stored in the real run, save file or random selector.
Random Event passes null and retains the existing weighted selection.

Map event nodes still enter `EncounterScreen` directly. They never use this chooser.
Selecting a preview does not change a later map encounter's probability or eligibility.
Completing or leaving a preview returns to the main menu as before.

## Removal

To remove just the chooser, delete `DemoEventPicker.java`, its listener in `MainMenuActions`,
and the picker field, helper, button branch and disposal hook in `MainMenuDisplay`.
The existing Demo Event button then calls `openDemoEvent()` directly again.
The public overload can be made private again. No normal gameplay code needs to change.
To hide the entire shortcut, remove its one button block from `MainMenuDisplay`.

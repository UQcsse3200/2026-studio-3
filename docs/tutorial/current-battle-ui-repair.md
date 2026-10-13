# Tutorial compatibility repair

Base: main commit `01b7dc31`. Review branch: `Fix-Sprint-3-Bug`.

The reference route is now Hand/Welcome, Card Inventory, Item Inventory, Energy,
Card Cost, successful demonstration Strike plus resolved damage, Enemy Health,
Enemy Armour, Player Health, End Turn, then Free Play. End Turn can be dismissed
by tapping to preserve remaining energy; using the real button waits for the
settled turn response. Energy bounds exclude transparent texture padding.
No Enemy Intent lesson is introduced. The legacy controller route is preserved.

Energy, player health and enemy stats use read-only accessors on the current UI
components. Inventory and End Turn use their existing clickable trigger records.
Targets are resolved on each layout pass. Numeric health text is also used to
observe when the displayed health catches up with combat resolution.

Exit Tutorial reuses the existing BattleMenuSkins END_TURN frame drawables and
font without taking ownership of the shared skin. It remains above the battle.

Verification on 8 October 2026: desktop compilation, 2,051 core tests (zero
failures/errors), formatting checks and git diff checks passed. New regression
tests cover Item Inventory order, numeric stat targets, live target movement,
and End Turn input handling. No game rules were changed.

Graphical acceptance remains to be performed: enter through New Game -> Enter
Tutorial, walk the ten lessons, play the highlighted Strike, confirm the real
damage result, click End Turn, test Exit, and repeat at different window sizes.
Check every arrow/highlight and ensure the menu prompts avoid the title.
Automated tests do not establish the final visual appearance.

# Sprint 4 — Joint review notes (Zeyu Wang / M5)

Status: **findings prepared for Teams 1 / 2 / 7**. No foreign systems edited in this branch. Agreed implementers TBD after those teams respond.

## Findings → candidate owners

| # | Finding | Evidence | Proposed change | Implementer (expected) | Status |
| --- | --- | --- | --- | --- | --- |
| 1 | Void Knight ATK 16 (22 @ progression 5) with low HP creates burst spike vs 100 HP / 3 energy | `enemies.json`, `EnemyScaling`, baselines doc | Soften early ATK or add telegraph / armour; or leave as elite identity | Team 1 | Open |
| 2 | Boss HP scales with NORMAL rates (+8%/+5%); long fights may be HP-pool length not threat | `EnemyScaling`, `boss_knight` 150→210 @ p5 | Separate boss curve or phase HP caps — only if playtests confirm stall | Team 1 | Open |
| 3 | `shopItems.json` prices unused; live shop uses rarity hardcodes | `ShopInventoryGenerator` | Document or wire JSON — avoid dual sources | Team 2 | Open |
| 4 | Chance fountain / dice outcomes are hardcoded; JSON deltas are placeholders | behaviour classes vs `chanceEncounters.json` | Align JSON with live values for designers | Team 2 | Open |
| 5 | Campfire rest is hardcoded 30% — no config knob for balance passes | `CampfireScreen.REST_HEAL_PERCENT` | Optional config if recovery feels wrong in playtests | Team 2 | Open |
| 6 | Max energy upgrades (Crystal / Temple Focus) not in `PlayerSaveData` | save restore gap in inventory | Persist max energy (and ideally owned items) | Team 5 + Team 7 | Open |
| 7 | Player JSON vs Java default gold (50 vs 10) | `player.json` / `PlayerConfig` | Align Java default to 50 to avoid silent fallback bugs | Team 7 | Open |
| 8 | `doom_sigil+` previously raised Vulnerable **value**; combat uses fixed 1.5× | `StatusEffectCalculator`, Card Upgrade Standard | Fixed on this branch: upgrade **duration** 2→3, keep value 1; Poison 4→6 | Team 6 (Zeyu) | **Done** |

## Coordination

- Card number tuning still goes through **Yu Zhu**; authors change their own cards.
- Reward weight values: Ray builds mechanism, Yu Zhu supplies weights — out of scope here.
- Team 7 confirmed Block/Armour rules stable this sprint; treat Block/Fortify bugs as correctness, not balance patches around them.

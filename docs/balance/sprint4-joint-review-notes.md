# Sprint 4 — Joint review notes (Zeyu Wang / M5)

Status: **findings prepared for Teams 1 / 2 / 7**. No foreign systems edited in this branch. Agreed implementers TBD after those teams respond.

## Findings → candidate owners

| # | Finding | Evidence | Proposed change | Implementer (expected) | Status |
| --- | --- | --- | --- | --- | --- |
| 1 | Void Knight ATK 16 (22 @ progression 5) with low HP creates burst spike vs 100 HP / 3 energy | `enemies.json`, `EnemyScaling`, baselines doc | Soften early ATK or add telegraph / armour; or leave as elite identity | Team 1 | Open |
| 2 | Boss HP scales with NORMAL rates (+8%/+5%); long fights may be HP-pool length not threat | `EnemyScaling`, `boss_knight` 150→210 @ p5 | Separate boss curve or phase HP caps — only if playtests confirm stall | Team 1 | Open |
| 1b | Enemy `DEFEND` intents call `CombatStatsComponent.addArmour` — Armour **persists and stacks** across turns until consumed (unlike player Block, which clears each turn). Defensive AIs (e.g. ArmourScaling defend 4, DefensiveStance defend 5) can snowball permanent Armour and stall fights | `EnemyBehaviourComponent.defend()`, `EnemyIntent.defend`, AI `DEFEND_AMOUNT` constants | Decide if enemy DEFEND should be Block-like (turn-clear), capped, or stay permanent Armour; document intended design | Team 1 | Open |
| 3 | `shopItems.json` prices unused; live shop uses rarity hardcodes | `ShopInventoryGenerator` | Document or wire JSON — avoid dual sources | Team 2 | Open |
| 4 | Chance fountain / dice outcomes are hardcoded; JSON deltas are placeholders | behaviour classes vs `chanceEncounters.json` | Align JSON with live values for designers | Team 2 | Open |
| 5 | Campfire rest is hardcoded 30% — no config knob for balance passes | `CampfireScreen.REST_HEAL_PERCENT` | Optional config if recovery feels wrong in playtests | Team 2 | Open |
| 6 | Max energy upgrades (Crystal / Temple Focus) not in `PlayerSaveData` | save restore gap in inventory | Persist max energy (and ideally owned items) | Team 5 + Team 7 | Open |
| 7 | Player JSON vs Java default gold (50 vs 10) | `player.json` / `PlayerConfig` | Align Java default to 50 to avoid silent fallback bugs | Team 7 | Open |
| 8 | `doom_sigil+` previously raised Vulnerable **value**; combat uses fixed 1.5× | `StatusEffectCalculator`, Card Upgrade Standard | Fixed on this branch: upgrade **duration** 2→3, keep value 1; Poison 4→6 | Team 6 (Zeyu) | **Done** |

## Status rules resolved on main

Checked on `main` @ `43474905` (19 Oct 2026). Tests below are in `ArmourAndStatusEffectTest` (file `components/ArmorAndStatusEffectTest.java`) and passed locally on JDK 21.

| Rule on main | Merged in | Covering tests | Effect on Team 6 cards |
| --- | --- | --- | --- |
| Strength gains **add** to the existing value and keep the longer duration; permanent (duration 0) wins | #489 (`c41809a7`) | `strengthReapplicationAddsToTheExistingValue`, `strengthReapplicationKeepsTheLongerDurationAndNotifiesEachTime`, `permanentStrengthWinsInEitherApplicationOrder` | Sealed Pact, Inner Focus and Astral Ward now stack. Boss-fight retest still pending on Yu Zhu's review page. |
| Vulnerable reapplication **never stacks intensity** (value stays 1) and keeps the **longer remaining duration**; permanent wins | #476 (`a716285d`) | `vulnerableReapplicationExtendsShorterDuration`, `vulnerableReapplicationUsesRemainingDurationWithoutStacking`, `permanentVulnerableWinsInEitherApplicationOrder`, `mergedVulnerableExpiresWithOneNotificationPerApplicationAndRemoval` | Doom Sigil+ (3 turns) is not cut short by a later 2-turn Expose / Poison Mark / Rift Lance. |

#476 also made Strength non-stacking; #489 replaced that part. The Vulnerable rule from #476 is unchanged.

## Coordination

- Card number tuning still goes through **Yu Zhu**; authors change their own cards.
- Reward weight values: Ray builds mechanism, Yu Zhu supplies weights — out of scope here.
- Team 7 confirmed Block/Armour rules stable this sprint; treat Block/Fortify bugs as correctness, not balance patches around them.

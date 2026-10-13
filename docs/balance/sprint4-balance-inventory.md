# Sprint 4 — Balance inventory (Zeyu Wang / M5)

Issue: #431 · Branch: `Feature6-zeyu-sprint4-balance` · Owner: Team 6 (inventory only; edits stay with owning teams)

Paths are relative to `source/core/`.

| Value | Default / example | Authoritative source | Loader / application | Owning team | Runtime notes |
| --- | --- | --- | --- | --- | --- |
| Player current HP | 100 | `assets/configs/player.json` → `health` | `PlayerFactory` → `CombatStatsComponent`; durable via `PlayerRunState` | Team 7 | Save: `PlayerSaveData.currentHealth`. Mid-run: combat, Chance, Campfire, Temple. |
| Player max HP | 100 | `player.json` → `maxHealth` | `PlayerFactory` / `PlayerRunState` | Team 7 | Temple Vitality +20. Save: `maxHealth`. |
| Player base attack | 10 | `player.json` → `baseAttack` | `PlayerFactory` | Team 7 | Not in save. |
| Starting gold | 50 | `player.json` → `gold` | `PlayerFactory` / `InventoryComponent` | Team 7 | Java `PlayerConfig` default is 10; JSON wins. |
| Max energy | 3 | `player.json` → `maxEnergy` | `EnergyComponent` via `PlayerFactory.resolveMaxEnergy` | Team 7 | Crystal / Temple Focus +1 via `RunState.playerMaxEnergy`. **Not saved** in `PlayerSaveData`. |
| Energy refill | full each turn | hardcoded | `EnergyComponent.onTurnStart` ← `BattleController` | Team 7 | No JSON. |
| Battle hand / opening draw | 5 | hardcoded `BattleScreen.AMOUNT_OF_CARDS_IN_DECK` | `BattleDeck.drawCards(5)` | Team 6 | No separate per-turn draw constant. |
| Starter deck | 11 cards | hardcoded `PlayerDeckFactory.STARTER_DECK_CARD_IDS` | `RunState.getOrCreatePlayerDeck` | Team 6 | Save restores via `DeckSaveData`. |
| Campfire rest heal | 30% max HP (min 1) | hardcoded `CampfireScreen.REST_HEAL_PERCENT` | `CampfireScreen.applyRest` | Team 2 | Caps at max HP. |
| Post-battle gold | 20–30 | hardcoded `RewardGenerator.MIN_GOLD` / `MAX_GOLD` | `RewardService` | Team 6 | Lucky Coin multiplies. |
| Card reward choices | up to 3 | `CardRewardSelection.MAX_CHOICES` + `assets/configs/cardAcquisitionPool.json` | `CardRewardSelectionGenerator` | Team 6 | Weights (normal/elite) owned by Ray + Yu Zhu this sprint. |
| Shop prices (live) | COMMON 20±5, UNCOMMON 35±5, RARE 50±5 | hardcoded `ShopInventoryGenerator` | shop screens | Team 2 | `shopItems.json` is **not** the production path. |
| Shop offers / stock | 3 offers, stock 1–3 | hardcoded `ShopInventoryGenerator` (`DEFAULT_OFFER_COUNT` / `MIN`/`MAX_STOCK`) | `ShopInventoryGenerator.createShop` | Team 2 | Live shop path; not from `shopItems.json`. |
| Chance HP / gold deltas | e.g. shrine −10/+25, healer +20/−10 | `assets/configs/chanceEncounters.json` | `ChanceOutcomeApplier` | Team 2 | Fountain / dice use behaviour hardcodes (JSON zeros). |
| Enemy base HP / ATK / armour | see `enemies.json` | `assets/configs/enemies.json` | `EnemyFactory` / `BattleGameArea` | Team 1 | Armour unscaled. |
| Enemy progression scaling | NORMAL/BOSS +8% HP / +5% ATK per step; ELITE +10% / +7% | hardcoded `EnemyScaling` | `BattleGameArea` | Team 1 | Overrides JSON bases at runtime. |
| Encounter line-ups | COMBAT / ELITE / FINAL | `assets/configs/encounters.json` | `BattleEncounterSelector` | Team 1 | Weights + progression gates. |
| Map room mix | combat 60 / event 20 / elite 10 / shop 10 | hardcoded `MapGenerationConfig` / `NodePoolGenerator` | `NodePoolGenerator` (campfire at `MAP_HEIGHT - 1`) | Team 2 | Campfire forced near top; drives encounter density. |
| Save player fields | HP, max HP, gold | save schema | `GameStateSnapshotProvider` / `SaveGameRestoreService` | Team 5 | Gaps: max energy, owned items (Lucky Coin, Merchant’s Favor, Energy Crystal). |

## Forbidden card upgrade values (own cards — for Yu Zhu)

| Card | Base | Upgrade |
| --- | --- | --- |
| `sealed_pact` | 3 Strength + 6 Block (cost 3) | 4 Strength + 9 Block |
| `blood_price` | 20 damage (cost 3) | 28 damage |
| `doom_sigil` | 1 Vulnerable / 2 turns + 4 Poison / 3 | **1 Vulnerable / 3 turns** + 6 Poison / 3 (duration, not value — matches upgrade standard) |
| `iron_oath` | 4 Fortify / Armour (cost 1) | 6 Fortify |

## Evidence

- Inventory: this document (config snapshot; owning teams update their values).
- Encounter pressure numbers: `docs/balance/sprint4-baselines.md` (doc snapshot only — not hard-locked in tests).
- Forbidden upgrades: `ForbiddenCardsTest` + `CardUpgradeStandardTest` (already on team branch).
- Full journey: `ForbiddenRewardPersistenceTest` (base + upgraded copies).

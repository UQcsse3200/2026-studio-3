# Sprint 4 — Full-journey verification (Zeyu Wang / M5)

Scope: production combat path for **claim Forbidden reward → save → load → next battle deck**, plus Forbidden upgrade-standard check. Does not change RewardDisplay UI, Ray’s inspect/reward weights, or Team 5 save schema.

**Related:** Forbidden card values and review decisions are on [Card Values and Balance Review](https://github.com/UQcsse3200/2026-studio-3/wiki/Card-Values-and-Balance-Review) (Yu Zhu).

## Automated evidence

| Step | Check | Test |
| --- | --- | --- |
| Claim card reward | `RewardService.claimRunReward` adds instance to run deck | `ForbiddenRewardPersistenceTest` |
| Save | `SaveGameService.saveGame(1)` succeeds | same |
| Load / restore | `SaveGameRestoreService.restore` restores same `instanceId` / `cardId` | same |
| Next battle | `BattleDeck` draw pile contains that instance | same |
| Covers all 4 Forbidden IDs | `sealed_pact`, `blood_price`, `doom_sigil`, `iron_oath` | same |
| Upgraded copy | `iron_oath` at `UPGRADED_LEVEL` survives save/load and enters next battle deck | `upgradedForbiddenCopySurvivesSaveRestoreAndEntersNextBattleDeck` |
| Upgrades meet standard | name+, rarity, cost, effect order, no weaken, description rewrite | `CardUpgradeStandardTest` + `ForbiddenCardsTest` |
| Vulnerable tip | `doom_sigil+` upgrades duration not value | `ForbiddenCardsTest.shouldConfigureDoomSigil` |
| Resolve upgraded copy | resolver returns upgraded numbers | `ForbiddenCardsTest.forbiddenUpgradesResolveToUpgradedValues` |

Related existing coverage (unchanged): `PostBattleCardRewardPersistenceTest` (generic bandage path), `AutosaveCoordinatorTest` (RewardDisplay durable path).

## Manual checklist (if UI inspect is required later)

Ray owns inspect UI. When available, confirm once:

1. Victory → inspect a Forbidden reward option (no reroll).
2. Claim → autosave.
3. Quit / load run.
4. Enter next combat and play that exact copy.

## Result

Automated journey for all four Forbidden cards (base + one upgraded copy): **pass** when `./gradlew :core:test --tests …ForbiddenRewardPersistenceTest` and Forbidden/upgrade standard tests are green on this branch.

Encounter pressure numbers stay in `docs/balance/sprint4-baselines.md` only (Team 1 owns the values; no CI hard-lock).

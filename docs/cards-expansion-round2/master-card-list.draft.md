# Master new-card list — DRAFT

Maintainer: Hezhenyu (Member 1), Team 6. Round 2: Cards Expansion.

Allocation source: the supplied “Team 6: Cards Expansion — ROUND 2 | TASK ALLOCATION | 5 MEMBERS” document (workspace copy: `../output/pdf/Team6-Round2-Cards-Expansion-English.md`, outside the repository). It assigns Member 1 Guardian, Member 2 Astral, Member 3 Corruption, Member 4 Survival and Member 5 Forbidden. Hezhenyu's Member 1 identity is user-confirmed. The source does not name Members 2–5; role labels are retained without guessing identities.

Exactly 20 Round 2 expansion slots: four per member. All four of Member 1's Guardian cards are implemented in the official config, leaving 16 expansion slots to implement. All other owners' statuses, card names and dependencies remain unconfirmed and require their input. This is a proposed coordination document, not a record of others' actual progress.

| Card Name | Owner | Theme | Status | Effect Dependencies | Notes |
| --- | --- | --- | --- | --- | --- |
| Warding Sweep (proposed) | Hezhenyu (Member 1) | Guardian | Implemented (in config) | DAMAGE; ALL_ENEMIES | In source/core/assets/configs/cards.json: cost 2, ATTACK, COMMON, ALL_ENEMIES, DAMAGE 4. Rarity confirmed COMMON; the design document has been aligned. |
| Sentinel's Rebuke (proposed) | Hezhenyu (Member 1) | Guardian | Implemented (in config); gameplay integration provisional | DAMAGE + FEEBLE; SINGLE_ENEMY | In source/core/assets/configs/cards.json: cost 1, ATTACK, UNCOMMON, SINGLE_ENEMY, DAMAGE 4 + FEEBLE 1 for 2 turns. Uses only existing effects. Config-loading and validation tests pass; live FEEBLE application and duration ticking remain integration dependencies. Artwork is a placeholder pending the agreed art specification. |
| Unseal the Breach (proposed) | Hezhenyu (Member 1) | Guardian | Implemented (in config); new effect sign-off in progress | DAMAGE + SUNDER; SINGLE_ENEMY | In source/core/assets/configs/cards.json: cost 1, ATTACK, UNCOMMON, SINGLE_ENEMY, DAMAGE 2 + SUNDER 3. SUNDER added to EffectType and handled in BattleController.applyEnemyEffects(), Team1EnemyStateAdapter.applyEnemyEffects() and both EffectExecutor enemy paths on commit 7fc8bfd. Config-loading, field, effect-order and validation tests pass. Team 5 has been asked for individual effect sign-off and currently treats SUNDER as provisional. Armour reduction is currently permanent because clearArmor() has no production call site. Artwork is a placeholder. |
| Warden's Judgement (proposed) | Hezhenyu (Member 1) | Guardian | Implemented (in config) | DAMAGE; SINGLE_ENEMY | In source/core/assets/configs/cards.json: cost 2, ATTACK, UNCOMMON, SINGLE_ENEMY, DAMAGE 9. Config-loading and validation tests pass. Artwork is a placeholder. Replaces the withdrawn Seal and Restore slot. |
| Seal and Restore (proposed) | Hezhenyu (Member 1) | Guardian | Withdrawn | BLOCK + HEAL; SELF | Block + heal role handed to Member 4 (Survival); expansion slot replaced by Warden's Judgement. |
| TBD — Astral 1 | Member 2 | Astral | Not started | TBD — owner to confirm | Unnamed allocation slot; no design assigned by Member 1. |
| TBD — Astral 2 | Member 2 | Astral | Not started | TBD — owner to confirm | Unnamed allocation slot; no design assigned by Member 1. |
| TBD — Astral 3 | Member 2 | Astral | Not started | TBD — owner to confirm | Unnamed allocation slot; no design assigned by Member 1. |
| TBD — Astral 4 | Member 2 | Astral | Not started | TBD — owner to confirm | Unnamed allocation slot; no design assigned by Member 1. |
| TBD — Corruption 1 | Member 3 | Corruption | Not started | TBD — owner to confirm | Unnamed allocation slot; no design assigned by Member 1. |
| TBD — Corruption 2 | Member 3 | Corruption | Not started | TBD — owner to confirm | Unnamed allocation slot; no design assigned by Member 1. |
| TBD — Corruption 3 | Member 3 | Corruption | Not started | TBD — owner to confirm | Unnamed allocation slot; no design assigned by Member 1. |
| TBD — Corruption 4 | Member 3 | Corruption | Not started | TBD — owner to confirm | Unnamed allocation slot; no design assigned by Member 1. |
| TBD — Survival 1 | Member 4 | Survival | Not started | TBD — owner to confirm | Unnamed allocation slot; no design assigned by Member 1. |
| TBD — Survival 2 | Member 4 | Survival | Not started | TBD — owner to confirm | Unnamed allocation slot; no design assigned by Member 1. |
| TBD — Survival 3 | Member 4 | Survival | Not started | TBD — owner to confirm | Unnamed allocation slot; no design assigned by Member 1. |
| TBD — Survival 4 | Member 4 | Survival | Not started | TBD — owner to confirm | Unnamed allocation slot; no design assigned by Member 1. |
| TBD — Forbidden 1 | Member 5 | Forbidden | Not started | TBD — owner to confirm | Unnamed allocation slot; no design assigned by Member 1. |
| TBD — Forbidden 2 | Member 5 | Forbidden | Not started | TBD — owner to confirm | Unnamed allocation slot; no design assigned by Member 1. |
| TBD — Forbidden 3 | Member 5 | Forbidden | Not started | TBD — owner to confirm | Unnamed allocation slot; no design assigned by Member 1. |
| TBD — Forbidden 4 | Member 5 | Forbidden | Not started | TBD — owner to confirm | Unnamed allocation slot; no design assigned by Member 1. |

The six original cards are excluded from these 20 expansion slots. The official config currently contains 10 cards: the six originals plus warding_sweep, sentinels_rebuke, wardens_judgement and unseal_the_breach, which fill four expansion slots. The remaining 16 expansion slots are not yet implemented. The eventual target is 26 official cards; local test examples and drafts do not count as delivered cards.

Dependencies must use the agreed target/effect matrix in [the draft template](card-design-template.draft.md). In particular, the allocation's illustrative Forbidden self-poison/self-vulnerability combinations are not supported by the confirmed SELF effect set and are not approved designs. Member 5 must select a supported design or confirm an extension with Team 5. Member 1's Unseal the Breach implements the new SUNDER effect on commit 7fc8bfd; Team 5 sign-off is in progress and the effect is treated as provisional until confirmed.

Guardian details: [four draft designs](guardian-card-designs.draft.md). Review all names and allocations before sharing with the group.

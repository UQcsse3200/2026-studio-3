# Master new-card list — DRAFT

Maintainer: Hezhenyu (Member 1), Team 6. Round 2: Cards Expansion.

Allocation source: the supplied “Team 6: Cards Expansion — ROUND 2 | TASK ALLOCATION | 5 MEMBERS” document (workspace copy: `output/pdf/Team6-Round2-Cards-Expansion-English.md`, outside the repository). It assigns Member 1 Guardian, Member 2 Astral, Member 3 Corruption, Member 4 Survival and Member 5 Forbidden. Hezhenyu's Member 1 identity is user-confirmed. The source does not name Members 2–5; role labels are retained without guessing identities.

Exactly 20 Round 2 expansion slots: four per member. Three of Member 1's four cards are implemented in the official config, leaving 17 expansion slots to implement. All other owners' statuses, card names and dependencies remain unconfirmed and require their input. This is a proposed coordination document, not a record of others' actual progress.

| Card Name | Owner | Theme | Status | Effect Dependencies | Notes |
| --- | --- | --- | --- | --- | --- |
| Warding Sweep (proposed) | Hezhenyu (Member 1) | Guardian | Implemented (in config) | DAMAGE; ALL_ENEMIES | In source/core/assets/configs/cards.json: cost 2, ATTACK, COMMON, ALL_ENEMIES, DAMAGE 4. Rarity confirmed COMMON; the design document has been aligned. |
| Sentinel's Stance (proposed) | Hezhenyu (Member 1) | Guardian | Implemented (in config) | BLOCK + STRENGTH; SELF | In source/core/assets/configs/cards.json: cost 2, SKILL, UNCOMMON, SELF, BLOCK 5 + STRENGTH 1. Config-loading and validation tests pass. Artwork is a placeholder pending the agreed art specification. |
| Unseal the Breach (proposed) | Hezhenyu (Member 1) | Guardian | Blocked — new effect pending Team 5 agreement | DAMAGE + SUNDER (NEW — not in EffectType); SINGLE_ENEMY | Design revised 2026-09-13; VULNERABLE replaced by the proposed SUNDER instant armour-reduction effect because the Vulnerable multiplier and duration expiry are not functioning consistently in the inspected paths. Not in cards.json. Requires agreement with Team 5 via Member 2 before implementation. |
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

The six original cards are excluded from these 20 expansion slots. The official config currently contains 9 cards: the six originals plus warding_sweep, sentinels_stance and wardens_judgement, which fill three expansion slots. The remaining 17 expansion slots are not yet implemented. The eventual target is 26 official cards; local test examples and drafts do not count as delivered cards.

Dependencies must use the agreed target/effect matrix in [the draft template](card-design-template.draft.md). In particular, the allocation's illustrative Forbidden self-poison/self-vulnerability combinations are not supported by the confirmed SELF effect set and are not approved designs. Member 5 must select a supported design or confirm an extension with Team 5. Member 1's Unseal the Breach likewise proposes a new SUNDER effect and requires agreement with Team 5 before implementation.

Guardian details: [four draft designs](guardian-card-designs.draft.md). Review all names and allocations before sharing with the group.

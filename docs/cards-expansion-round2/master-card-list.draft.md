# Master new-card list — DRAFT

Maintainer: Hezhenyu (Member 1), Team 6. Round 2: Cards Expansion.

Allocation source: the supplied “Team 6: Cards Expansion — ROUND 2 | TASK ALLOCATION | 5 MEMBERS” document (workspace copy: `output/pdf/Team6-Round2-Cards-Expansion-English.md`, outside the repository). It assigns Member 1 Guardian, Member 2 Astral, Member 3 Corruption, Member 4 Survival and Member 5 Forbidden. Hezhenyu's Member 1 identity is user-confirmed. The source does not name Members 2–5; role labels are retained without guessing identities.

Exactly 20 Round 2 expansion slots: four per member. Warding Sweep is implemented in the official config, leaving 19 slots to implement, including a pending redesign for the withdrawn Seal and Restore slot. All other statuses remain **Not started**, pending review and implementation. Other owners' names, card names and dependencies require their input. This is a proposed coordination document, not a record of others' actual progress.

| Card Name | Owner | Theme | Status | Effect Dependencies | Notes |
| --- | --- | --- | --- | --- | --- |
| Warding Sweep (proposed) | Hezhenyu (Member 1) | Guardian | Implemented (in config) | DAMAGE; ALL_ENEMIES | In source/core/assets/configs/cards.json: cost 2, ATTACK, COMMON, ALL_ENEMIES, DAMAGE 4. Rarity confirmed COMMON; the design document has been aligned. |
| Sentinel's Stance (proposed) | Hezhenyu (Member 1) | Guardian | Not started | BLOCK + STRENGTH; SELF | Design draft only; not approved or implemented. |
| Unseal the Breach (proposed) | Hezhenyu (Member 1) | Guardian | Not started | DAMAGE + VULNERABLE; SINGLE_ENEMY | Design draft only; not approved or implemented. |
| Seal and Restore (proposed) | Hezhenyu (Member 1) | Guardian | Withdrawn | BLOCK + HEAL; SELF | Block + heal role handed to Member 4 (Survival); slot awaits a redesigned Guardian card. |
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

The six original cards are excluded from these 20 expansion slots. The official config currently contains 7 cards: the six originals plus warding_sweep, which fills one expansion slot. The remaining 19 slots include the Guardian replacement pending redesign. The eventual target is 26 official cards; local test examples and drafts do not count as delivered cards.

Dependencies must use the agreed target/effect matrix in [the draft template](card-design-template.draft.md). In particular, the allocation's illustrative Forbidden self-poison/self-vulnerability combinations are not supported by the confirmed SELF effect set and are not approved designs. Member 5 must select a supported design or confirm an extension with Team 5.

Guardian details: [four draft designs](guardian-card-designs.draft.md). Review all names and allocations before sharing with the group.

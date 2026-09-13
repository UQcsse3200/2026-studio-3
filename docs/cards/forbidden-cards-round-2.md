# Forbidden Cards — Round 2

## Scope

Member 5 owns four Forbidden Ritual cards and coordinates integration and demonstration for the
Round 2 card expansion. This document records the card designs, balance rationale, test-deck entry
point, verification checklist, known dependencies and repeatable demonstration steps.

The four cards use effects already represented by the Team 6 card model and currently supported by
the combat integration:

- `SELF`: `STRENGTH`, `BLOCK`, `HEAL`
- Enemy targets: `DAMAGE`, `POISON`, `VULNERABLE`, `FEEBLE`

The data model can store other target/effect combinations, but that does not prove the combat
executor can resolve them. In particular, these cards do not depend on self-targeted `POISON`,
`VULNERABLE` or `FEEBLE`.

## New Effect — FORTIFY (Member 5)

Round 2 also requires one new `EffectType` per member. Member 5 adds:

| Field | Value |
| --- | --- |
| EffectType | `FORTIFY` |
| usesDuration | `false` |
| Valid targets | `SELF` only |
| Behaviour | Immediately adds armour via `CombatStatsComponent.addArmor(value)` |
| Strength | Does not modify FORTIFY |

Companion card: `iron_oath` — cost 1, `SKILL`, `UNCOMMON`, `SELF`, `FORTIFY 4` (upgrade `FORTIFY 6`).

Armour is distinct from per-turn Block. This effect does not depend on `updateStatusEffects()`.

## Artwork standard

Card PNGs are **illustration-only** inside an ornate frame:

- Size: `691×1056`
- Allowed UI baked into the image: cost orb digit only
- Name, type, rarity and rules text come from `cards.json` / Team 3 UI overlay — **not** painted into
  the PNG

## Card Designs

### Sealed Pact

| Field | Value |
| --- | --- |
| ID | `sealed_pact` |
| Cost | 3 |
| Type / rarity | `POWER` / `RARE` |
| Target | `SELF` |
| Effects | `STRENGTH 3`, `BLOCK 6` |
| Upgrade | `STRENGTH 4`, `BLOCK 9` (same cost) |
| Artwork | `images/cards/sealed_pact.png` |

**Theme:** The player binds their fate to the corrupted archive, gaining lasting power while
spending most of the current turn completing the ritual.

**Intended use:** Starts the Strength/burst line and provides enough immediate block to reduce the
risk of spending three energy on setup.

**Balance rationale:** Compared with `inner_focus` (2 energy for 2 Strength), it costs one more
energy for one more Strength and 6 Block. Its drawback is the high upfront cost and lack of direct
damage during the setup turn. The upgrade keeps cost 3 and increases both Strength and Block.

### Blood Price

| Field | Value |
| --- | --- |
| ID | `blood_price` |
| Cost | 3 |
| Type / rarity | `ATTACK` / `UNCOMMON` |
| Target | `SINGLE_ENEMY` |
| Effects | `DAMAGE 20` |
| Upgrade | `DAMAGE 28` (same cost) |
| Artwork | `images/cards/blood_price.png` |

**Theme:** Forbidden power is released in one decisive strike.

**Intended use:** A high-impact attack for the Strength/burst line, particularly after `Sealed
Pact`.

**Balance rationale:** Its 20 damage is efficient in card usage but consumes three energy and
normally prevents other plays that turn. The upgrade raises damage to 28 without changing cost.

### Doom Sigil

| Field | Value |
| --- | --- |
| ID | `doom_sigil` |
| Cost | 2 |
| Type / rarity | `SKILL` / `RARE` |
| Target | `SINGLE_ENEMY` |
| Effects | `VULNERABLE 1` for 2 turns; `POISON 4` for 3 turns |
| Upgrade | `VULNERABLE 2` for 2 turns; `POISON 6` for 3 turns |
| Artwork | `images/cards/doom_sigil.png` |

**Theme:** A forbidden mark exposes an enemy to attacks while corruption consumes it over time.

**Intended use:** Opens the poison/control line and prepares a priority target for follow-up
attacks.

**Balance rationale:** It applies two useful statuses but deals no immediate damage and costs two
energy. The upgrade increases both status stacks while keeping cost and durations.

### Eclipse Decree

| Field | Value |
| --- | --- |
| ID | `eclipse_decree` |
| Cost | 3 |
| Type / rarity | `SKILL` / `RARE` |
| Target | `ALL_ENEMIES` |
| Effects | `FEEBLE 1` for 2 turns |
| Upgrade | `FEEBLE 1` for 3 turns |
| Artwork | `images/cards/eclipse_decree.png` |

**Theme:** The player invokes an eclipse that weakens every enemy in the archive.

**Intended use:** A group-control option for encounters with several enemies.

**Balance rationale:** A fixed Feeble effect reduces affected enemies' outgoing damage by 25%.
Applying it to all enemies is powerful, so the card costs three energy and causes no direct damage.
The upgrade extends duration rather than stacking Feeble, because the live multiplier does not scale
with stack count.

## Upgrade schema

Upgrades use the optional `upgrade` object (not `upgradedEffects`):

```json
"upgrade": {
  "name": "Card Name+",
  "description": "...",
  "cost": 3,
  "rarity": "RARE",
  "effects": [{ "type": "DAMAGE", "value": 28, "duration": 0 }]
}
```

Upgraded variants inherit ID, type, target and artwork from the base card.

## Set Interactions and Playstyles

### Strength / burst

1. Play `Sealed Pact` to gain 3 Strength and temporary Block.
2. Play `Blood Price` on a later turn.
3. Confirm Strength increases the resolved attack damage.

This line trades an expensive setup turn for stronger later attacks.

### Poison / control

1. Play `Doom Sigil` on a priority enemy.
2. Use the Vulnerable window for follow-up attacks while Poison applies delayed damage.
3. Use `Eclipse Decree` in a multi-enemy encounter to reduce incoming damage.

This line gives up immediate damage and energy for status pressure and safer future turns.

## Test-Deck Entry Point

Formal reward/shop acquisition is an external integration dependency. Until that route is
confirmed, Team 6 provides this clearly labelled test-only entry point:

```java
PlayerDeck testDeck = PlayerDeckFactory.createForbiddenTestDeck();
```

The deterministic ten-card test deck contains all four Forbidden cards plus supporting copies of
`strike` and `defend`. The normal `createStarterDeck()` method is unchanged.

The battle screen currently calls `createStarterDeck()`. Connecting the test deck to a debug menu
or launch option remains pending; production code should not silently replace the starter deck.

## Integration Checklist

### Configuration and resources

- [x] Four unique IDs use lower snake case.
- [x] Every required `CardConfig` field is present.
- [x] Optional `upgrade` blocks defined for all four Forbidden cards.
- [x] JSON syntax is valid.
- [x] Effect values and durations satisfy `CardValidator` rules.
- [x] Four artwork files exist at their configured paths.
- [x] All four IDs are accepted by `CardIdRegistry`.
- [x] Artwork is 691×1056 illustration-only (cost digit only; no baked name/rules text).
- [ ] Confirm no ID or design overlap with the Member 1 master inventory.

### Automated verification

- [x] `ForbiddenCardsTest` checks loading, registration, base fields and upgrade fields.
- [x] `PlayerDeckFactoryTest` covers test-deck contents and immutable ID snapshots.
- [x] Targeted Spotless checks pass for newly modified Java files.
- [ ] Run the card and deck tests with JDK 21.
- [ ] Run the complete project test suite and record the result after all 20 cards merge.
- [ ] Confirm the final official configuration contains 26 unique cards.

### Display and combat

- [ ] Display every Forbidden card in the actual hand UI.
- [ ] Check card art is not cropped and UI overlay text is readable.
- [ ] Confirm three energy is spent for `Sealed Pact`, `Blood Price` and `Eclipse Decree`.
- [ ] Confirm two energy is spent for `Doom Sigil`.
- [ ] Confirm `SINGLE_ENEMY`, `SELF` and `ALL_ENEMIES` target selection.
- [ ] Confirm ordered resolution of both effects on `Sealed Pact` and `Doom Sigil`.
- [ ] Confirm Strength modifies `Blood Price` damage.
- [ ] Confirm Poison and Vulnerable durations update correctly.
- [ ] Confirm Feeble applies to every enemy for two turns and uses the fixed 0.75 multiplier.
- [ ] Confirm played cards move from hand to discard and replacement cards are drawn.
- [ ] Confirm upgrade path applies when the combat/upgrade system is available.

### Acquisition

- [ ] Agree with the reward/shop team how new cards enter the player's deck.
- [ ] Verify acquisition modifies the same `PlayerDeck` used to begin combat.
- [x] Provide `createForbiddenTestDeck()` while production acquisition is pending.
- [ ] Replace or retain the test entry point according to the final integration decision.

## Tracked Issues and Dependencies

| Item | Owner | Status / required action |
| --- | --- | --- |
| Reward/shop acquisition route | Reward/shop team + Member 5 | Pending agreement and end-to-end verification |
| Debug selection of Forbidden test deck | Member 5 / battle UI owner | Factory entry exists; UI/launch selection not connected |
| Art specification | Member 5 (this set) | 691×1056 illustration-only; cost orb digit only |
| Effect support | Member 2 + Team 5/7 | Existing effects selected; verify actual combat behaviour |
| Upgrade runtime | Card upgrade owners | JSON `upgrade` filled; live upgrade application depends on shared systems |
| Final 26-card inventory | Member 1 + all authors | Merge all member submissions and check unique IDs |
| Full automated tests | Member 5 | Requires JDK 21 and final merged configuration |

## Repeatable Demonstration

1. Build and launch the project using JDK 21.
2. Create the battle deck from `PlayerDeckFactory.createForbiddenTestDeck()` through the agreed
   debug/test selection.
3. Enter combat and confirm all four Forbidden cards can appear in hand.
4. Demonstrate `Sealed Pact` followed by `Blood Price`, recording energy use, Strength and resolved
   damage.
5. Demonstrate `Doom Sigil`, recording the target, Vulnerable duration and Poison duration.
6. In a multi-enemy encounter, demonstrate `Eclipse Decree` and confirm every enemy receives
   Feeble.
7. Confirm played cards move to discard and hand replacement still works.
8. Record screenshots or video and note any deviations in the issue table.

This is a test entry route, not evidence that production reward/shop acquisition is complete.

## AI-Assisted Artwork Disclosure

The four card images were generated with Cursor's image-generation tool, using an illustration-only
frame reference (cost digit only; no title or rules text). Zeyu Wang selected the card concepts,
mechanics, text in `cards.json`, upgrade values and final assets. This use must also be included in
the team's AI declaration and any required wiki attribution.

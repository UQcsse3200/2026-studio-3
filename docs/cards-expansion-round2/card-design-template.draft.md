# Shared card-design template — DRAFT

Owner: Hezhenyu (Member 1), Team 6. Round 2: Cards Expansion.
Review status: proposed for review; not an approved team standard or implementation.
Basis: user-confirmed Step 1 schema and effect constraints.

## Naming and artwork

- Use unique lowercase snake_case IDs, such as `warding_sweep`. Check uniqueness across the existing six cards and all proposed new cards.
- No theme prefix is required or agreed. Any future prefix convention needs group agreement before adoption.
- Proposed artwork convention: `images/cards/<id>.png`, relative to `source/core/assets/`; the filename must match the card ID exactly, including case.
- A proposed artwork path does not mean the asset exists. Confirm dimensions, composition and UI text treatment with Member 4 before producing final artwork.

## Copyable submission checklist

### CardConfig fields (exact field names)

- [ ] `id` — unique nonblank String, lowercase snake_case, no surrounding whitespace.
- [ ] `name` — nonblank String; English display name.
- [ ] `description` — String; clear English rules text matching effects, values, targets and durations.
- [ ] `cost` — int, zero or greater; energy cost.
- [ ] `type` — CardType: ATTACK, SKILL, POWER, STATUS or CURSE.
- [ ] `rarity` — Rarity: COMMON, UNCOMMON or RARE.
- [ ] `target` — TargetType: SELF, SINGLE_ENEMY or ALL_ENEMIES.
- [ ] `effects` — nonempty EffectConfig[]; list effects in intended resolution order.
- [ ] `texturePath` — nonblank String; proposed asset-relative path following the naming rule above.

### Each EffectConfig entry (exact field names)

Repeat this checklist for every entry in `effects`:

- [ ] `type` — EffectType: DAMAGE, BLOCK, HEAL, POISON, VULNERABLE, FEEBLE or STRENGTH.
- [ ] `value` — int, strictly positive; magnitude appropriate to the effect.
- [ ] `duration` — int; positive for POISON, VULNERABLE and FEEBLE; exactly 0 for DAMAGE, BLOCK, HEAL and STRENGTH.

`duration` is per effect, not a CardConfig field. `target` is card-wide, not per effect. Every effect on a card must support that card's target:

- SELF: BLOCK, HEAL, STRENGTH.
- SINGLE_ENEMY or ALL_ENEMIES: DAMAGE, POISON, VULNERABLE, FEEBLE.

### Design-review notes (documentation only; not extra config fields)

- Owner / member number and theme:
- Short archive-background concept:
- Intended role and play sequence:
- Mechanical difference from existing cards:
- Interaction with another card in the owner's set:
- Cost and balance rationale, including enemy-count scaling:
- Known effect/integration dependencies:
- Artwork status:
- Review feedback and outstanding decisions:

## Effect and targeting rules

VULNERABLE is the agreed mechanism for “weaken enemy defense”: it increases damage taken by 50%. It does not remove armor or provide a new armor-break effect. Do not describe a greater `value` as a greater percentage bonus without a confirmed change to the mechanics.

**SINGLE_ENEMY targeting has an open question with Team 1/Team 5 — avoid designs that assume guaranteed single-target precision until confirmed.**

FEEBLE exists in the schema/executor, but BattleController's enemy-effect path may omit it. Record this unresolved dependency for any proposed FEEBLE card; do not represent it as verified gameplay support.

## Do / don't

- Do use exact field and enum names, valid effect-target combinations and positive effect values.
- Do explain how rules text maps to each effect and record unverified integration behavior.
- Do keep draft concepts and local test examples separate from the official card count.
- Do confirm new effects with Team 5 before including them in implementation scope.
- Don't invent armor break, card draw, energy refunds, conditional triggers or per-effect targets.
- Don't combine enemy damage and self block on a single card under the current schema.
- Don't infer executable mechanics from CardType names or flavor text.
- Don't treat a proposed texture path, accepted design or tracking row as completed implementation.

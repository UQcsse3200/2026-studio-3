# Shared card-design template — DRAFT

Owner: Hezhenyu (Member 1), Team 6. Round 2: Cards Expansion.
Review status: proposed for review; not an approved team standard or implementation.
Basis: user-confirmed Step 1 schema and effect constraints, Team 5's intended contract (#173), Team 7's duration integration status (#161), and artwork specification agreed with Ziqin.

## Naming and artwork

- Use unique lowercase snake_case IDs, such as `warding_sweep`. Check uniqueness across the existing six cards and all proposed new cards.
- No theme prefix is required or agreed. Any future prefix convention needs group agreement before adoption.
- Artwork specification agreed with Ziqin: dimensions **691×1056**.
- Produce a **full card face for now**: `BattleScreen` draws the whole PNG as the hand button at **225×456** and does not overlay the name, cost or rules. Illustration-only artwork is a follow-up once the UI supports text rendering.
- Save artwork at `source/core/assets/images/cards/<id>.png`, referenced in JSON as `images/cards/<id>.png`; the filename must match the card ID exactly, including case.
- A proposed artwork path does not mean the asset exists. Confirm the asset is present before marking artwork complete.

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

VULNERABLE is the agreed mechanism for “weaken enemy defense”: it increases damage taken by 50%. It does not remove armor or provide a new armor-break effect. `EffectConfig.value` is a no-op for VULNERABLE and FEEBLE: their damage multipliers are fixed at ×1.5 and ×0.75 respectively. The schema still requires a strictly positive `value`. Do not describe a greater `value` as a greater percentage bonus without a confirmed change to the mechanics.

**Team 5 has confirmed the intended contract (#173). Design cards against the unified `CardPlayService` / `CardPlayResult` flow, not the temporary buggy `BattleController` path.**

- `SINGLE_ENEMY` applies only to the selected enemy; `ALL_ENEMIES` applies to all living enemies.
- `VULNERABLE` applies for AoE as well as single-target.
- `FEEBLE` is handled consistently in the unified path.
- `BLOCK` and Armor are separate concepts; `BLOCK` must not go through `addArmor()`.
- Landing the unified flow remains an integration dependency tracked in #173; the intended contract is confirmed, but this does not establish that the current live path implements it correctly.

**Effect durations do NOT currently tick.** Team 7's #161 covers wiring `updateStatusEffects()` into the battle lifecycle. Delivery is targeted before September 15 but is not confirmed. Treat any card whose value depends on POISON, VULNERABLE or FEEBLE lasting multiple turns as **provisional** until that integration lands.

## Do / don't

- Do use exact field and enum names, valid effect-target combinations and positive effect values.
- Do explain how rules text maps to each effect and record unverified integration behavior.
- Do keep draft concepts and local test examples separate from the official card count.
- Do confirm new effects with Team 5 before including them in implementation scope.
- Don't invent armor break, card draw, energy refunds, conditional triggers or per-effect targets.
- Don't combine enemy damage and self block on a single card under the current schema.
- Don't infer executable mechanics from CardType names or flavor text.
- Don't treat a proposed texture path, accepted design or tracking row as completed implementation.

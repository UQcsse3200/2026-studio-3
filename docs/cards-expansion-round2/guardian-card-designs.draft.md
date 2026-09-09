# Guardian card concepts — DRAFT revision 2

Owner: Hezhenyu (Member 1), Team 6. Theme: archive guardian techniques.
Design only; all names, numbers and rarities await review. Implementation status: Not started. No card configuration or artwork is created by this document. Each texturePath below is proposed, not an existing-asset claim.

## Part A — source verification

Inspected the source on task/guardian-cards, based on fbb0cfe. Line references below describe this snapshot. This is static source evidence, not a gameplay-test result.

### Actual card-play path

BattleScreen.java lines 121–128 constructs BattleController with CardEffectResolver. BattleController.java line 674 calls `effectResolver.resolve(card, playerEffectState)`. Its direct consumer loops over living enemies (lines 715–746), calls `stats.takeDamage(effect.value())`, and stores poison/vulnerable statuses. It does not use the context-based Vulnerable/Feeble calculation. CombatStatsComponent.takeDamage (lines 130–134) absorbs block, then armor, then subtracts health; it does not apply status multipliers.

A separate CardPlayService context-based path exists. Lines 235–245 read Strength and outgoing Feeble, but read target Vulnerable only when `target.type() == TargetType.SINGLE_ENEMY`. ALL_ENEMIES therefore receives no target Vulnerable multiplier through this context builder. Do not claim Vulnerable boosts Sweep in either of these inspected card-play paths.

### STRENGTH

- EffectExecutor.java line 159: `Math.max(0, effect.value + playerState.getStrength())`. This is a flat additive bonus per DAMAGE effect record. Context-based resolution agrees: CardEffectResolutionContext.java line 35 uses `Math.max(0, baseDamage + strength)` before multipliers, and line 45 rounds the final multiplied result down.
- AoE: the resolved amount is computed once per effect, then applied in full to EACH enemy. BattleController.java lines 729–736: `for (Entity enemy : targets)` followed by `case DAMAGE -> stats.takeDamage(effect.value());`. Team1EnemyStateAdapter.java lines 77–81 likewise loops through selected enemies and applies that same amount. Strength is not divided across targets or cumulatively re-added during iteration. Each separate DAMAGE entry would receive the bonus separately.
- Magnitude: EffectExecutor.java line 128 calls `playerState.addStrength(effect.value)`; PlayerEffectState.java line 32 performs `strength += amount`. Thus value controls the bonus gained in the BattleController path, with additive reapplications.
- Duration: STRENGTH has usesDuration=false; EffectExecutor.java lines 120–121 reject nonzero duration. PlayerEffectState has no duration countdown. CardEffectResolutionService.java lines 95–99 explicitly preserves Strength when clearing turn results.
- Alternate path discrepancy: Team7PlayerStateAdapter.java lines 64–65 calls `combatStats.applyStatusEffect(effect.type().name(), effect.value(), effect.duration())`. CombatStatsComponent.java line 337 replaces an existing same-key status. Repeated Strength applications therefore overwrite instead of accumulate through this adapter.
- Lifetime: BattleController.java line 90 creates a new PlayerEffectState per controller. Strength persists across its plays/turns. Its resetBattle method (lines 243–259) does not clear this state. In generic StatusEffect, duration 0 never expires from ticking. “Rest of combat” is the intended contract, but cleanup on every possible battle reset/reuse is not established by this code.

### VULNERABLE

- CardEffectResolutionContext.java line 18: `VULNERABLE_DAMAGE_MULTIPLIER = 1.5`; lines 41–42: `if (targetVulnerable > 0)` then multiply by that constant. Positive values 1 and 2 produce the same +50% multiplier; value is not a percentage or scaling factor.
- Value is still validated as positive, stored and used as an activation check in this context. It is not globally meaningless. The separate StatusEffectCalculator.java lines 29–31 checks only whether the uppercase status exists, also returning a fixed 1.5 multiplier.
- Reapplication: CombatStatsComponent.java line 337 uses `statusEffects.put(effect.getType(), effect)`. For the exact same key, the new value AND duration replace the old ones. It neither sums values nor adds durations nor preserves the longest duration. A shorter new duration can shorten the remaining effect.
- Duration: StatusEffect.java lines 101–106 return false for nonpositive duration; otherwise `duration--` and expire at zero. CombatStatsComponent.java lines 393–403 remove expired entries. This counts update calls, not independently scheduled turns.
- Wiring gap: production-source search found no caller of updateStatusEffects outside its definition. Its lines 389–391 explicitly say turn timing is not yet wired. Therefore a configured duration of 2 expresses the intended two-turn debuff, but actual two-turn expiry is not confirmed.
- BattleController.java lines 739–742 stores lowercase `poison`/`vulnerable`; helpers/adapters use uppercase names. CombatStatsComponent.java line 362 uses `statusEffects.get(type)` without normalization. These are different keys. This compounds the missing multiplier path described above.

### FEEBLE

- CardEffectResolutionContext.java line 17: `FEEBLE_DAMAGE_MULTIPLIER = 0.75`; lines 38–39 apply it if `outgoingFeeble > 0`. This is fixed -25% outgoing damage; larger positive values do not increase the penalty. StatusEffectCalculator.java lines 18–20 instead tests status presence and returns the same fixed multiplier.
- Value is stored/validated and supplies the context activation check; it does not scale the multiplier. Same-key applications replace value and duration via the same map operation described above.
- Positive duration uses the same decrement/expiry helper as Vulnerable, with the same missing turn wiring. No confirmed number of real turns can be promised from that helper alone.
- Team1EnemyStateAdapter.java lines 82–83 applies FEEBLE; BattleController.java lines 735–745 omits it. The presence of the enum/helper does not prove enemy attacks receive the penalty in the active battle path. No Guardian concept below uses FEEBLE.

### POISON

- EffectExecutor.java lines 164–166 pass value and duration through unchanged. Team1EnemyStateAdapter.java lines 82–83 passes them into the stored status.
- StatusEffectCalculator.java line 45: `return Math.max(poison.getValue(), 0);`. The helper uses value directly as the poison damage amount. “Stacks” in EffectConfig/StatusEffect comments is terminology, not an implemented additive stacking rule.
- Same-key reapplication overwrites value and duration; no additive poison stacking is implemented by these consumers.
- Production-source search found no caller of getPoisonDamage outside its definition and no wired poison-damage tick. Consequently actual damage per tick, tick timing, and number of ticks are NOT confirmed. Duration counts expiry-helper calls if invoked, not verified poison ticks. The helper decrements duration only; it does not decrement poison value.

### Source locations

All paths below are relative to this document:

- [CardEffectResolutionContext.java](../../source/core/src/main/com/csse3200/game/cards/effects/CardEffectResolutionContext.java)
- [EffectExecutor.java](../../source/core/src/main/com/csse3200/game/cards/effects/EffectExecutor.java)
- [PlayerEffectState.java](../../source/core/src/main/com/csse3200/game/cards/effects/PlayerEffectState.java)
- [CardEffectResolutionService.java](../../source/core/src/main/com/csse3200/game/cards/effects/CardEffectResolutionService.java)
- [CardPlayService.java](../../source/core/src/main/com/csse3200/game/cards/play/CardPlayService.java)
- [Team1EnemyStateAdapter.java](../../source/core/src/main/com/csse3200/game/cards/play/integration/Team1EnemyStateAdapter.java)
- [Team7PlayerStateAdapter.java](../../source/core/src/main/com/csse3200/game/cards/play/integration/Team7PlayerStateAdapter.java)
- [BattleScreen.java](../../source/core/src/main/com/csse3200/game/screens/BattleScreen.java)
- [BattleController.java](../../source/core/src/main/com/csse3200/game/components/combat/BattleController.java)
- [CombatStatsComponent.java](../../source/core/src/main/com/csse3200/game/components/CombatStatsComponent.java)
- [StatusEffect.java](../../source/core/src/main/com/csse3200/game/components/StatusEffect.java)
- [StatusEffectCalculator.java](../../source/core/src/main/com/csse3200/game/components/StatusEffectCalculator.java)

## Part B — revised designs

Descriptions state intended card rules. Part A's integration gaps remain unresolved and must be verified before gameplay acceptance. No new effects, per-effect targets, conditional bonuses or precision-dependent nukes are introduced.

### 1. Warding Sweep — deliberate area attack

- `id`: `warding_sweep`
- `name`: Warding Sweep
- `description`: Deal 4 damage to all enemies.
- `cost`: 2
- `type`: ATTACK
- `rarity`: UNCOMMON
- `target`: ALL_ENEMIES
- `effects[]`: one entry — `type: DAMAGE`, `value: 4`, `duration: 0`.
- `texturePath`: `images/cards/warding_sweep.png`
- Archive technique: a broad blade arc along the perimeter of a ward.
- Balance rationale: raised from 1 to 2 energy. Against one/two/three enemies, unmodified total damage is 4/8/12 for 2 energy; two Strikes give 12 total for the same energy if available. Sweep trades concentrated damage and energy flexibility for encounter-wide coverage and one-card convenience. Its advantage at larger enemy counts is deliberate but still needs playtesting, especially with Strength; rarity alone is not the balancing lever.
- Difference from Strike: area coverage at twice the cost with lower damage per enemy, rather than a numerical upgrade to a focused attack. Difference from Defend: immediate offensive pressure with no protection.

### 2. Sentinel's Stance — defense with offensive preparation

- `id`: `sentinels_stance`
- `name`: Sentinel's Stance
- `description`: Gain 5 block. Gain 1 Strength for the rest of combat.
- `cost`: 2
- `type`: SKILL
- `rarity`: UNCOMMON
- `target`: SELF
- `effects[]`, in order:
  1. `type: BLOCK`, `value: 5`, `duration: 0`.
  2. `type: STRENGTH`, `value: 1`, `duration: 0`.
- `texturePath`: `images/cards/sentinels_stance.png`
- Archive technique: brace behind an engraved shield and recover a disciplined attacking stance.
- Balance rationale: lowered block from 6 to 5. Relative to Defend, the extra energy buys 1 Strength; relative to Inner Focus at the same cost, it trades 1 of the 2 Strength for 5 immediate block. It is a hybrid, not a better replacement for either baseline. Do not assume repeated plays accumulate Strength consistently across paths.
- Difference from Defend: adds a persistent offensive preparation effect at a higher cost. Difference from Strike: no immediate damage, with value realized through later attacks.
- Integration note: intended combat-long Strength matches the contract, but reset cleanup differs as documented in Part A. BLOCK also becomes armor in BattleController versus block in Team7PlayerStateAdapter; do not claim a verified expiry time for its protection.

### 3. Unseal the Breach — modest single-enemy opening

- `id`: `unseal_the_breach`
- `name`: Unseal the Breach
- `description`: Deal 2 damage to an enemy. Apply 1 Vulnerable to that enemy for 1 turn.
- `cost`: 1
- `type`: ATTACK
- `rarity`: UNCOMMON
- `target`: SINGLE_ENEMY
- `effects[]`, in order:
  1. `type: DAMAGE`, `value: 2`, `duration: 0`.
  2. `type: VULNERABLE`, `value: 1`, `duration: 1`.
- `texturePath`: `images/cards/unseal_the_breach.png`
- Archive technique: cut a small opening through an intruder's protective inscription.
- Balance rationale: lowered cost from 2 to 1, changed to SINGLE_ENEMY, and shortened Vulnerable from 2 turns to 1. Compared with Strike, it trades 4 immediate damage for debuff setup. Compared with Expose at the same cost, it trades enemy-wide coverage and one turn of debuff duration for 2 immediate damage. Unlike Poison Dagger, it offers neither 4 immediate damage nor poison's advertised long-term damage; its niche is enabling follow-up damage. Poison's baseline is a configuration reference, not verified ticks.
- Precision tolerance: the base hit is only 2. If the existing bug spreads it to all enemies, the comparison with Expose is 2 area damage plus a shorter intended debuff, rather than a huge nuke or a free equal-duration upgrade. This does not declare bugged behavior balanced at every enemy count; correct targeting and duration handling still need acceptance checks.
- Difference from Strike: combines a modest hit with a debuff, rather than concentrating on immediate damage. Difference from Defend: weakens enemy defense through increased damage taken, with no block. It does not remove or bypass armor.
- Integration dependency: the Vulnerable multiplier and intended one-turn expiration are not functioning consistently in the inspected paths. This card satisfies the requested design role but is blocked from gameplay acceptance until those integrations are resolved. The primary set interaction below does not depend on it. Damage is listed before Vulnerable; no same-card amplification is assumed.

### 4. Seal and Restore — protection-heavy recovery

- `id`: `seal_and_restore`
- `name`: Seal and Restore
- `description`: Gain 7 block. Heal 4 health.
- `cost`: 2
- `type`: SKILL
- `rarity`: UNCOMMON
- `target`: SELF
- `effects[]`, in order:
  1. `type: BLOCK`, `value: 7`, `duration: 0`.
  2. `type: HEAL`, `value: 4`, `duration: 0`.
- `texturePath`: `images/cards/seal_and_restore.png`
- Archive technique: reinforce a damaged ward while restoring its keeper.
- Balance rationale: raised from BLOCK 4 / HEAL 3 to BLOCK 7 / HEAL 4. Defend + Bandage provides BLOCK 5 / HEAL 6 for the same 2 energy. This card explicitly trades 2 healing for 2 additional block, so it is no longer lower in both values. One-card convenience is an advantage; paying both energy together and reduced recovery are disadvantages. Block and healing are not assumed to be interchangeable in every state. Compared with two Defends, it sacrifices 3 block for 4 healing. Healing may be wasted near full health; repeated sustain and overlap with Survival designs need review.
- Difference from Defend: combines stronger immediate protection with recovery at a higher cost. Difference from Strike: affects both defensive resources without dealing damage. Unlike Bandage it allocates much of its benefit to protection.

## Verified core interaction: Sentinel's Stance → Warding Sweep

Starting from zero Strength, one Stance grants 1 Strength. A subsequent Sweep resolves to `4 + 1 = 5` damage per enemy before mitigation, instead of 4. With two living targets, the consumers receive two 5-damage hits rather than two 4-damage hits. This is confirmed by the calculation and target loops in Part A; it is not a claim of a completed gameplay test.

The sequence combines protection/preparation with later area offense. Both cards cost 2; no same-turn affordability is assumed. Use one Stance application for this example so it does not depend on the additive-versus-overwrite discrepancy. Existing higher Strength, enemy count and repeated plays require balance testing. Block/armor can reduce actual HP loss.

The previous Vulnerable → Sweep interaction is withdrawn: the inspected active BattleController does not apply that multiplier, and the alternate CardPlayService only reads Vulnerable for SINGLE_ENEMY. No substitute damage bonus is invented.

## Review boundary

All four designs remain proposals. Keep cards.json and combat source unchanged. Review the numbers, overlap with other themes, and Part A integration dependencies before authorizing implementation. The master list's older ALL_ENEMIES note for Breach predates this revision; this document is the current proposal. That separate draft has not been edited in this revision request.

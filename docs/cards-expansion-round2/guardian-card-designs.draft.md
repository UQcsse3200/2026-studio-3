# Guardian card concepts — DRAFT revision 4

Owner: Hezhenyu (Member 1), Team 6. Theme: archive guardian techniques.
Design only; all names, numbers and rarities await review. Implementation status: Warding Sweep, Sentinel's Rebuke, Warden's Judgement and Unseal the Breach are in source/core/assets/configs/cards.json with placeholder artwork. Artwork paths below point to placeholder assets pending the agreed art specification.

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
- Team1EnemyStateAdapter.java lines 82–83 applies FEEBLE; BattleController.java lines 735–745 omits it. The presence of the enum/helper does not prove enemy attacks receive the penalty in the active battle path. Sentinel's Rebuke now uses FEEBLE under the confirmed unified-flow contract, but its live gameplay behaviour remains provisional until the integration and duration wiring land.

### POISON

- EffectExecutor.java lines 164–166 pass value and duration through unchanged. Team1EnemyStateAdapter.java lines 82–83 passes them into the stored status.
- StatusEffectCalculator.java line 45: `return Math.max(poison.getValue(), 0);`. The helper uses value directly as the poison damage amount. “Stacks” in EffectConfig/StatusEffect comments is terminology, not an implemented additive stacking rule.
- Same-key reapplication overwrites value and duration; no additive poison stacking is implemented by these consumers.
- Production-source search found no caller of getPoisonDamage outside its definition and no wired poison-damage tick. Consequently actual damage per tick, tick timing, and number of ticks are NOT confirmed. Duration counts expiry-helper calls if invoked, not verified poison ticks. The helper decrements duration only; it does not decrement poison value.

### ARMOUR (SUNDER target mechanic)

- CombatStatsComponent.java stores armour in the private `int armor` field and exposes `public int getArmor()` and `public void setArmor(int)`. The setter clamps the supplied value to zero or greater and triggers the entity's `updateArmor` event.
- CombatStatsComponent.java applies incoming damage through `absorbDamageWithBlock(damage)`, then `absorbDamageWithArmor(afterBlock)`, then subtracts the remainder from health. Armour therefore mitigates damage after block and before health.
- EnemyFactory.java constructs each enemy's CombatStatsComponent from configured health and base attack, then initializes armour with `stats.setArmor(config.armour)`.
- CombatStatsComponent.java documents armour as follows: “It persists until consumed by incoming damage or explicitly cleared via clearArmor() - it does not reset automatically at any point in the turn cycle.” Production-source search found no caller of `clearArmor()` outside its definition.
- EnemyBehaviourComponent.java adds armour with `stats.addArmor(currentIntent.getValue())` when an enemy executes its DEFEND intent. EnemyConfig.java supplies `cycle_attack_defend` as the default behaviour, so enemies using that default can regain armour during combat rather than receiving a per-turn reset.
- source/core/assets/configs/enemies.json assigns initial armour 0 to `lesser_shade`, 2 to `bone_crawler`, 1 to `dark_acolyte`, and 5 to `void_knight`. EnemyScaling.java copies `base.armour` unchanged into the scaled config.
- Armour is a dedicated CombatStatsComponent integer field, not a StatusEffect entry. Its initialization, consumption and DEFEND-based replenishment therefore do not depend on the unwired `updateStatusEffects()` turn lifecycle described above.

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
- [EnemyFactory.java](../../source/core/src/main/com/csse3200/game/entities/factories/EnemyFactory.java)
- [EnemyBehaviourComponent.java](../../source/core/src/main/com/csse3200/game/components/enemy/EnemyBehaviourComponent.java)
- [StatusEffect.java](../../source/core/src/main/com/csse3200/game/components/StatusEffect.java)
- [StatusEffectCalculator.java](../../source/core/src/main/com/csse3200/game/components/StatusEffectCalculator.java)

## Part B — revised designs

Descriptions state intended card rules. Part A's integration gaps remain unresolved and must be verified before gameplay acceptance. SUNDER is one proposed new effect and is explicitly blocked on the dependency recorded below; no per-effect targets, conditional bonuses or precision-dependent nukes are introduced.

### 1. Warding Sweep — deliberate area attack

- `id`: `warding_sweep`
- `name`: Warding Sweep
- `description`: Deal 4 damage to all enemies.
- `cost`: 2
- `type`: ATTACK
- `rarity`: COMMON
- `target`: ALL_ENEMIES
- `effects[]`: one entry — `type: DAMAGE`, `value: 4`, `duration: 0`.
- `texturePath`: `images/cards/warding_sweep.png`
- Archive technique: a broad blade arc along the perimeter of a ward.
- Rarity decision (2026-09-12): confirmed COMMON and aligned to the shipped config value, because this is a plain DAMAGE 4 area attack with no additional mechanic.
- Balance rationale: raised from 1 to 2 energy. Against one/two/three enemies, unmodified total damage is 4/8/12 for 2 energy; two Strikes give 12 total for the same energy if available. Sweep trades concentrated damage and energy flexibility for encounter-wide coverage and one-card convenience. Its advantage at larger enemy counts is deliberate but still needs playtesting, especially with Strength; rarity alone is not the balancing lever.
- Difference from Strike: area coverage at twice the cost with lower damage per enemy, rather than a numerical upgrade to a focused attack. Difference from Defend: immediate offensive pressure with no protection.

### 2. Sentinel's Rebuke — protection through suppression

- `id`: `sentinels_rebuke`
- `name`: Sentinel's Rebuke
- `description`: Deal 4 damage. Apply 1 Feeble for 2 turns.
- `cost`: 1
- `type`: ATTACK
- `rarity`: UNCOMMON
- `target`: SINGLE_ENEMY
- `effects[]`, in order:
  1. `type: DAMAGE`, `value: 4`, `duration: 0`.
  2. `type: FEEBLE`, `value: 1`, `duration: 2`.
- `texturePath`: `images/cards/sentinels_rebuke.png`
- Archive technique: answer an intruder's advance with a measured shield strike that disrupts the force of its next attacks.
- Balance rationale: Strike deals 6 damage for the same 1 energy. Rebuke gives up 2 immediate damage for the intended two-turn 25% outgoing-damage reduction from FEEBLE. FEEBLE's positive value activates a fixed multiplier rather than scaling its percentage, so value 1 is sufficient. The duration and overall value remain subject to playtesting once status ticking is integrated.
- Difference from Strike: trades one-third of Strike's base damage for temporary offensive suppression. Difference from Defend: reduces one selected enemy's future pressure rather than granting immediate protection, so target choice and enemy intent matter.
- Difference from Astral cards: unlike Rift Lance's DAMAGE + VULNERABLE offensive setup and Astral Ward's BLOCK + STRENGTH self-buff, Rebuke combines damage with an enemy outgoing-damage penalty.
- Integration note: DAMAGE resolves in the inspected paths. FEEBLE is an existing EffectType and is accepted by the unified resolver and Team1EnemyStateAdapter, so this design adds no new effect. The active legacy BattleController omits FEEBLE, enemy attacks do not yet consult the status multiplier, and effect durations do not tick in production; gameplay acceptance remains provisional on those existing integration dependencies.

### 3. Unseal the Breach — focused armour opening

- `id`: `unseal_the_breach`
- `name`: Unseal the Breach
- `description`: Deal 2 damage to an enemy. Reduce that enemy's armour by 3.
- `cost`: 1
- `type`: ATTACK
- `rarity`: UNCOMMON
- `target`: SINGLE_ENEMY
- `effects[]`, in order:
  1. `type: DAMAGE`, `value: 2`, `duration: 0`.
  2. `type: SUNDER`, `value: 3`, `duration: 0`.
- `texturePath`: `images/cards/unseal_the_breach.png`
- Archive technique: cut a small opening through an intruder's protective inscription.
- Revision note (2026-09-13): replaced VULNERABLE with the proposed SUNDER effect. Part A documents that the Vulnerable multiplier and duration expiry do not function consistently in the inspected paths. SUNDER is intended to resolve immediately with duration 0, so its design does not depend on the `updateStatusEffects()` wiring gap.
- Balance rationale: SUNDER 3 fully removes the initial armour of `bone_crawler` (2) and `dark_acolyte` (1), and removes 3 of `void_knight`'s 5. `lesser_shade` has 0 armour, so SUNDER is wasted against it; selecting this card and its target is therefore a real decision rather than a strictly better attack. Enemies can regain armour through their DEFEND intent, making armour reduction repeatable in value rather than only a one-time strip. Compared with Strike at the same 1-energy cost, Unseal the Breach trades 4 immediate damage for removing a persistent damage-reduction pool. Compared with Expose at the same 1-energy cost, Expose is intended to cover ALL_ENEMIES with a Vulnerable damage multiplier, subject to the integration gaps in Part A; this card instead proposes concentrating on one enemy and permanently reducing its current armour pool, without preventing later DEFEND-based replenishment.
- Resolution order — DESIGN INTENT, NOT VERIFIED: the 2 DAMAGE entry resolves before SUNDER. Existing block and armour can absorb that hit; SUNDER then reduces the selected enemy's remaining armour by 3, clamped at zero through CombatStatsComponent.setArmor. Precise single-enemy application remains an acceptance requirement.
- Difference from Strike: exchanges 4 immediate damage for persistent single-enemy armour reduction and improved follow-up damage. Difference from Defend: removes an enemy's mitigation instead of adding protection to the player.
- **NEW-EFFECT DEPENDENCY — BLOCKED:** SUNDER does not exist in EffectType. Adding it requires agreement with Team 5 through Member 2 under the Round 2 allocation. The required code changes are: add a SUNDER EffectType constant with `usesDuration = false`; add SUNDER acceptance to both EffectExecutor.resolveEnemyEffect overloads; add a SUNDER branch to BattleController.applyEnemyEffects; and add a SUNDER branch to Team1EnemyStateAdapter.applyEnemyEffects. This card is blocked from gameplay acceptance until that agreement is recorded. SUNDER must not be presented as implemented or verified before those changes land and are tested.
- Scope note: SUNDER is the deliberate armour-reduction mechanic proposed for the Guardian set. Round 2 instructs authors not to assume an armour-break mechanic already exists; the verified findings show that no card effect currently supplies one, so this card proposes the extension rather than assuming existing support.

### 4. Warden's Judgement — dedicated single-target payoff

- `id`: `wardens_judgement`
- `name`: Warden's Judgement
- `description`: Deal 9 damage to an enemy.
- `cost`: 2
- `type`: ATTACK
- `rarity`: UNCOMMON
- `target`: SINGLE_ENEMY
- `effects[]`, in order:
  1. `type: DAMAGE`, `value: 9`, `duration: 0`.
- `texturePath`: TBD — artwork pending the agreed art specification; no asset path assigned.
- Archive technique: a single sanctioned strike delivered by the warden against one intruder. The blow carries the authority of the archive's protective inscriptions.
- Intended role: the set's dedicated single-target damage card and the payoff for setup provided by the other Guardian cards. It fills the fourth slot left by the withdrawn Seal and Restore.
- Balance rationale: Strike costs 1 energy for 6 damage. Two Strikes give 12 damage for 2 energy but use two cards; Warden's Judgement gives 9 for the same energy on one card, trading 3 damage for card economy. Against Warding Sweep at the same cost, it offers 9 to one target versus 4 per target (4/8/12 against one/two/three enemies). One enemy favours Warden's Judgement, three favour Warding Sweep, and two is a genuine decision point between concentrated damage and area coverage. This is a different role, not a numerical variant. The starting value of 9 is provisional and subject to Member 3's balance review, which owns the cost-versus-benefit baseline.
- Intra-set interaction — DESIGN INTENT, NOT VERIFIED: Sentinel's Rebuke lowers a selected enemy's future damage while dealing 4 immediate damage, buying room to follow with Warden's Judgement's concentrated 9-damage hit. Unseal the Breach proposes SUNDER to remove armour before the payoff card is played, allowing more of Warden's Judgement's damage to reach health. The Rebuke protection depends on the existing FEEBLE integration gaps; the SUNDER interaction is implemented but not yet verified in live gameplay.
- Difference from Strike: higher single-target damage at twice the cost, trading energy flexibility for card economy. Difference from Defend: pure offence, no protection.
- Integration note: the card uses DAMAGE only. DAMAGE is confirmed resolvable through the active BattleScreen → BattleController path, so its standalone damage does not depend on status-effect integration. This does not verify precise single-target behaviour: Part A's active consumer applies enemy-facing effects to all living enemies, so the intended SINGLE_ENEMY targeting still requires acceptance verification. Sentinel's Rebuke's FEEBLE setup remains subject to the status integration gaps above, while the SUNDER setup is implemented and unit-tested but not yet verified in live gameplay, and Team 5 sign-off for the effect is still in progress.
- Scope note: Warden's Judgement remains the Guardian set's dedicated single-target damage payoff. Unseal the Breach now proposes the set's separate armour-reduction role through SUNDER; Warden's Judgement itself does not reduce or bypass armour.

The withdrawn section below retains its original slot number and text for audit; Warden's Judgement is the active fourth design.

### Withdrawn — Seal and Restore

- status: Withdrawn
- reason: block + heal falls under Member 4's Survival scope (Round 2 allocation); collides with Member 4's Makeshift Shelter draft (2 cost, SELF, BLOCK 7 + HEAL 3)
- date: 2026-09-12
- replacement: TBD — fourth Guardian card to be redesigned in the defence / defence-weakening space

Original design retained below for audit:

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

## Provisional core sequence: Sentinel's Rebuke → Warden's Judgement

Rebuke deals 4 damage to the selected enemy and is intended to reduce that enemy's outgoing damage by 25% for two turns. The protection creates room to spend 2 energy on Warden's Judgement's 9 focused damage on a later turn. This is role complementarity rather than a direct damage multiplier.

The resolver can emit Rebuke's DAMAGE and FEEBLE records without a new EffectType, and the unified enemy adapter accepts both. The sequence is not yet a verified live gameplay interaction because the legacy BattleController omits FEEBLE, enemy attack damage does not currently consume its modifier, and status durations are not wired to turn progression.

The previous Sentinel's Stance BLOCK + STRENGTH design is withdrawn because it duplicated Astral Ward's cost, type, rarity, target and effect combination, differing only by one point of Block.

## Review boundary

Warden's Judgement is the replacement fourth Guardian design following the withdrawal of Seal and Restore; the withdrawn proposal is retained above for audit. Keep cards.json and combat source unchanged. Review the numbers, overlap with other themes, and Part A integration dependencies before authorizing implementation. The master list's older ALL_ENEMIES and VULNERABLE notes for Breach predate this revision; the current proposal is SINGLE_ENEMY with the new SUNDER dependency documented above. That separate draft has not been edited in this revision request.

package com.csse3200.game.components.enemy;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectCalculator;
import com.csse3200.game.components.enemy.EnemyAI.EnemyAI;
import com.csse3200.game.components.enemy.EnemyAI.EnemyAIContext;
import com.csse3200.game.components.enemy.EnemyAI.EnemyAIFactory;
import com.csse3200.game.components.enemy.Memory.EnemyMemoryComponent;
import com.csse3200.game.components.enemy.Memory.PlayerMemory;
import com.csse3200.game.entities.Entity;

/**
 * Decides and telegraphs an enemy's action each round, then resolves it.
 *
 * <p>The action for each round is chosen by an {@link EnemyAI} resolved from the enemy's behaviour
 * id. Player health is not yet visible to this component.
 */
public class EnemyBehaviourComponent extends Component {
  private static final int UNKNOWN_PLAYER_HEALTH = 0;

  private final String behaviourId;
  private final EnemyAI ai;
  private EnemyIntent currentIntent = EnemyIntent.unknown();
  private int turnNumber = 0;
  private CombatStatsComponent playerStats;
  private EnemyMemoryComponent enemyMemory;

  /**
   * Creates a behaviour that resolves its AI from a behaviour identifier.
   *
   * @param behaviourId behaviour identifier loaded from enemy configuration
   */
  public EnemyBehaviourComponent(String behaviourId) {
    this(behaviourId, EnemyAIFactory.create(behaviourId));
  }

  /**
   * Creates a behaviour with a given AI, bypassing the factory.
   *
   * <p>Package-private so tests and future behaviours can supply an AI directly instead of routing
   * through {@link EnemyAIFactory}. Production code should use the single-argument constructor so
   * behaviour stays configuration-driven.
   *
   * @param behaviourId behaviour identifier recorded for reference
   * @param ai the AI that decides this enemy's intents
   */
  EnemyBehaviourComponent(String behaviourId, EnemyAI ai) {
    this.behaviourId = behaviourId;
    this.ai = ai;
  }

  public String getBehaviourId() {
    return behaviourId;
  }

  /**
   * Supplies the player's combat stats so intents can react to the player's condition.
   *
   * <p>Injected rather than looked up, because this component lives on the enemy entity and has no
   * reference to the player. Until it is supplied, the AI context reports an unknown player health.
   *
   * @param playerStats the player's combat stats, or null to clear them
   */
  public void setPlayerStats(CombatStatsComponent playerStats) {
    this.playerStats = playerStats;
  }

  /**
   * Supplies the shared enemy-memory component attached to the player.
   *
   * <p>All enemies may read the same memory component, but each enemy still owns an independent AI
   * instance and makes its own decision.
   *
   * @param enemyMemory memory component attached to the player, or null to return to empty player
   *     memory
   */
  public void setEnemyMemory(EnemyMemoryComponent enemyMemory) {
    this.enemyMemory = enemyMemory;
  }

  /**
   * @return the intent telegraphed for the coming round
   */
  public EnemyIntent getCurrentIntent() {
    return currentIntent;
  }

  /**
   * Decides the action for the coming round and telegraphs it.
   *
   * <p>An enemy missing its stats component cannot make a meaningful decision, so it telegraphs
   * {@link EnemyIntent#unknown()} instead.
   *
   * @return the newly decided intent
   */
  public EnemyIntent rollIntent() {
    turnNumber++;

    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    currentIntent = stats == null ? EnemyIntent.unknown() : ai.decide(buildContext(stats));

    entity.getEvents().trigger("intentChanged", currentIntent);
    return currentIntent;
  }

  /**
   * Snapshots the battle state for the AI.
   *
   * <p>Player health is not yet available to this component, so it is reported as {@link
   * #UNKNOWN_PLAYER_HEALTH}.
   *
   * @param stats the enemy's own combat stats
   * @return a snapshot of the current battle state
   */
  private EnemyAIContext buildContext(CombatStatsComponent stats) {
    PlayerMemory playerMemory = enemyMemory == null ? PlayerMemory.empty() : enemyMemory.snapshot();

    return new EnemyAIContext(
        playerStats == null ? UNKNOWN_PLAYER_HEALTH : playerStats.getHealth(),
        stats.getHealth(),
        stats.getMaxHealth(),
        stats.getBaseAttack(),
        stats.getArmour(),
        currentIntent,
        turnNumber,
        playerMemory);
  }

  /**
   * Resolves the current intent against the given target.
   *
   * @param target the entity the intent acts on, typically the player
   */
  public void executeIntent(Entity target) {
    switch (currentIntent.getType()) {
      case ATTACK -> attack(target);
      case DEFEND -> defend();
      case DEBUFF -> applyDebuff(target);
      default -> {
        // No behaviour produces BUFF yet; UNKNOWN is intentionally inert.
      }
    }
  }

  /**
   * Deals damage equal to the telegraphed intent's value, not a freshly recomputed base attack.
   *
   * <p>Kept equal to {@code getBaseAttack()} for every current AI, so this is behaviour-preserving
   * for them; it only starts to matter for an AI (such as an armour-to-damage trade) whose intent
   * value differs from the plain base attack, letting the damage actually dealt match what was
   * telegraphed to the player.
   *
   * @param target the entity being attacked
   */
  private void attack(Entity target) {
    if (target == null) {
      return;
    }

    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    if (targetStats != null) {
      targetStats.takeDamage(outgoingDamage());
    }
  }

  /**
   * Scales the telegraphed damage by this enemy's outgoing modifier, so effects like Feeble
   * actually reduce what it deals.
   *
   * <p>The intent keeps its original value, so what was telegraphed to the player is unchanged.
   *
   * @return the damage this attack should deal
   */
  private int outgoingDamage() {
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    if (stats == null) {
      return currentIntent.getValue();
    }

    float modifier = StatusEffectCalculator.getOutgoingDamageModifier(stats);
    return Math.round(currentIntent.getValue() * modifier);
  }

  private void defend() {
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    if (stats != null) {
      entity.getEvents().trigger("enemyDefend");
      stats.addArmour(currentIntent.getValue());
    }
  }

  /**
   * Applies the intent's status effect to the target.
   *
   * <p>The effect type is converted to a string here because {@link CombatStatsComponent} stores
   * active effects in a string-keyed map. An intent without an effect type is ignored rather than
   * treated as an error, so a misconfigured behaviour does not break the battle.
   *
   * @param target the entity the effect is applied to
   */
  private void applyDebuff(Entity target) {
    if (target == null) {
      return;
    }

    IntentEffectType effectType = currentIntent.getEffectType();
    if (effectType == null) {
      return;
    }

    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    if (targetStats != null) {
      entity.getEvents().trigger("enemyCast");

      String statusKey =
          effectType == IntentEffectType.TAUNT
              ? IntentEffectType.TAUNT.name() + ":" + entity.getId()
              : effectType.name();
      int statusValue =
          effectType == IntentEffectType.TAUNT ? entity.getId() : currentIntent.getValue();

      targetStats.applyStatusEffect(statusKey, statusValue, currentIntent.getDuration());
    }
  }
}

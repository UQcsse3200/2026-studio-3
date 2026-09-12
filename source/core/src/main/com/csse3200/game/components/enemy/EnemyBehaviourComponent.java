package com.csse3200.game.components.enemy;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.enemy.EnemyAI.EnemyAI;
import com.csse3200.game.components.enemy.EnemyAI.EnemyAIContext;
import com.csse3200.game.components.enemy.EnemyAI.EnemyAIFactory;
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
    return new EnemyAIContext(
        playerStats == null ? UNKNOWN_PLAYER_HEALTH : playerStats.getHealth(),
        stats.getHealth(),
        stats.getMaxHealth(),
        stats.getBaseAttack(),
        stats.getArmor(),
        currentIntent,
        turnNumber);
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

  private void attack(Entity target) {
    if (target == null) {
      return;
    }

    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    CombatStatsComponent attackerStats = entity.getComponent(CombatStatsComponent.class);

    if (targetStats != null && attackerStats != null) {
      targetStats.hit(attackerStats);
    }
  }

  private void defend() {
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    if (stats != null) {
      stats.addArmor(currentIntent.getValue());
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
      targetStats.applyStatusEffect(
          effectType.name(), currentIntent.getValue(), currentIntent.getDuration());
    }
  }
}

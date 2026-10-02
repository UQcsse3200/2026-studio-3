package com.csse3200.game.components.enemy;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;

/**
 * Enemy-specific state and lifecycle event translation.
 *
 * <p>Combat stats (health, base attack, armour) live on the entity's {@link CombatStatsComponent}
 * so that other teams can read them uniformly with {@code
 * getComponent(CombatStatsComponent.class)}. This component holds only enemy-specific data
 * (currently the display name) and re-emits the enemy lifecycle events ({@code enemyDamaged},
 * {@code enemyDefeated}, {@code enemyEnraged}) from the shared {@code updateHealth} event fired by
 * {@link CombatStatsComponent}.
 */
public class EnemyStatsComponent extends Component {
  private static final String DEFAULT_DISPLAY_NAME = "Unknown Enemy";
  // Triggers the enemyEnraged event once when health falls to or below this proportion of max
  // health.
  private static final double ENRAGE_HEALTH_THRESHOLD = 0.3;

  private final String displayName;
  private int lastHealth;
  private boolean enraged;

  public EnemyStatsComponent() {
    this(DEFAULT_DISPLAY_NAME);
  }

  public EnemyStatsComponent(String displayName) {
    this.displayName =
        displayName == null || displayName.isBlank() ? DEFAULT_DISPLAY_NAME : displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  @Override
  public void create() {
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    lastHealth = stats == null ? 0 : stats.getHealth();
    entity.getEvents().addListener("updateHealth", this::onHealthUpdated);
  }

  private void onHealthUpdated(int health, int maxHealth) {
    if (health < lastHealth) {
      entity.getEvents().trigger("enemyDamaged", lastHealth - health);
    }

    // Check for enrage only while the enemy is alive. A killing blow is handled by enemyDefeated
    // below and must not also trigger enemyEnraged.
    if (!enraged && health > 0 && health <= maxHealth * ENRAGE_HEALTH_THRESHOLD) {
      enraged = true;
      entity.getEvents().trigger("enemyEnraged");
    }

    if (lastHealth > 0 && health == 0) {
      entity.getEvents().trigger("enemyDefeated");
    }
    lastHealth = health;
  }
}

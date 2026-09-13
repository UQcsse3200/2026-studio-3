package com.csse3200.game.components.enemy;

import com.csse3200.game.components.CombatStatsComponent;

/**
 * Health, attack and armour for an enemy, and the resolution of incoming damage.
 *
 * <p>Damage is currently untyped; typed damage can be added as an overload without breaking
 * callers.
 */
public class EnemyStatsComponent extends CombatStatsComponent {
  private static final String DEFAULT_DISPLAY_NAME = "Unknown Enemy";
  // 血量跌破上限的这个比例时触发一次"激怒"事件
  private static final double ENRAGE_HEALTH_THRESHOLD = 0.3;

  private final int maxHealth;
  private final String displayName;
  private int armour;
  private boolean enraged;

  public EnemyStatsComponent(int health, int baseAttack, int armour) {
    this(health, baseAttack, armour, DEFAULT_DISPLAY_NAME);
  }

  public EnemyStatsComponent(int health, int baseAttack, int armour, String displayName) {
    super(health, baseAttack);
    this.maxHealth = health;
    this.armour = Math.max(armour, 0);
    this.displayName =
        displayName == null || displayName.isBlank() ? DEFAULT_DISPLAY_NAME : displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public int getMaxHealth() {
    return maxHealth;
  }

  public int getArmour() {
    return armour;
  }

  public void setArmour(int armour) {
    this.armour = Math.max(armour, 0);
  }

  /**
   * Adds armour, for example when a defend intent resolves.
   *
   * <p>护甲真的增加时才广播 {@code enemyDefended} 事件，方便战斗特效监听。
   */
  public void addArmour(int armour) {
    int before = this.armour;
    setArmour(this.armour + armour);

    int gained = this.armour - before;
    if (gained > 0 && entity != null) {
      entity.getEvents().trigger("enemyDefended", gained);
    }
  }

  public boolean isAlive() {
    return getHealth() > 0;
  }

  /**
   * Applies damage, depleting armour before health.
   *
   * @param damage incoming damage, ignored if not positive
   */
  public void takeDamage(int damage) {
    if (damage <= 0 || !isAlive()) {
      return;
    }

    int healthBeforeDamage = getHealth();

    int absorbed = Math.min(armour, damage);
    setArmour(armour - absorbed);

    int remaining = damage - absorbed;
    if (remaining > 0) {
      setHealth(getHealth() - remaining);
    }

    int actualHealthDamage = healthBeforeDamage - getHealth();
    if (actualHealthDamage > 0 && entity != null) {
      entity.getEvents().trigger("enemyDamaged", actualHealthDamage);
    }

    // 只在活着的时候判断激怒，死亡这一击直接走下面的 enemyDefeated，不重复触发
    if (!enraged && isAlive() && getHealth() <= maxHealth * ENRAGE_HEALTH_THRESHOLD) {
      enraged = true;
      if (entity != null) {
        entity.getEvents().trigger("enemyEnraged");
      }
    }

    if (healthBeforeDamage > 0 && !isAlive() && entity != null) {
      entity.getEvents().trigger("enemyDefeated");
    }
  }
}

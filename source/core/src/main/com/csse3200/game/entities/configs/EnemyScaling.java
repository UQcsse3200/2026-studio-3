package com.csse3200.game.entities.configs;

/** Applies progression-based stat scaling to enemy configurations. */
public final class EnemyScaling {
  /**
   * Creates a scaled copy of an enemy configuration based on run progression.
   *
   * <p>Normal enemies gain 8% health and 5% attack per progression step. Elite enemies gain 10%
   * health and 7% attack per progression step. Negative progression values are treated as zero. The
   * original configuration is not modified.
   *
   * @param base the base enemy configuration
   * @param progression the current run progression
   * @return a new enemy configuration containing the scaled stats
   */
  public static EnemyConfig scale(EnemyConfig base, int progression) {
    int safeProgression = Math.max(0, progression);

    double healthRate;
    double attackRate;

    if (base.tier == EnemyTier.ELITE) {
      healthRate = 0.10;
      attackRate = 0.07;
    } else {
      healthRate = 0.08;
      attackRate = 0.05;
    }

    EnemyConfig scaled = new EnemyConfig();

    scaled.id = base.id;
    scaled.name = base.name;
    scaled.health = (int) Math.round(base.health * (1.0 + healthRate * safeProgression));
    scaled.baseAttack = (int) Math.round(base.baseAttack * (1.0 + attackRate * safeProgression));
    scaled.armour = base.armour;
    scaled.tier = base.tier;
    scaled.behaviour = base.behaviour;
    scaled.sprite = base.sprite;

    return scaled;
  }

  private EnemyScaling() {
    throw new IllegalStateException("Instantiating utility class");
  }
}

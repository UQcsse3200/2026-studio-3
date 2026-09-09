package com.csse3200.game.bestiary;

import com.csse3200.game.entities.configs.EnemyTier;
import java.util.Objects;

/** Read-only enemy information presented by the bestiary. */
public final class BestiaryEntry {
  private final String enemyId;
  private final String displayName;
  private final EnemyTier tier;
  private final String description;
  private final String imagePath;
  private final int maxHealth;
  private final int baseAttack;
  private final int armour;
  private final BestiaryUnlockState unlockState;

  /**
   * Creates a bestiary entry.
   *
   * @param enemyId stable enemy identifier
   * @param displayName player-facing enemy name
   * @param tier enemy difficulty tier
   * @param description player-facing bestiary description
   * @param imagePath internal path to the enemy portrait
   * @param maxHealth enemy base maximum health
   * @param baseAttack enemy base attack
   * @param armour enemy base armour
   * @param unlockState current bestiary progress state
   */
  public BestiaryEntry(
      String enemyId,
      String displayName,
      EnemyTier tier,
      String description,
      String imagePath,
      int maxHealth,
      int baseAttack,
      int armour,
      BestiaryUnlockState unlockState) {
    if (enemyId == null || enemyId.isBlank()) {
      throw new IllegalArgumentException("enemyId cannot be blank");
    }
    this.enemyId = enemyId;
    this.displayName = Objects.requireNonNull(displayName, "displayName cannot be null");
    this.tier = Objects.requireNonNull(tier, "tier cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.imagePath = Objects.requireNonNull(imagePath, "imagePath cannot be null");
    this.maxHealth = maxHealth;
    this.baseAttack = baseAttack;
    this.armour = armour;
    this.unlockState = Objects.requireNonNull(unlockState, "unlockState cannot be null");
  }

  public String getEnemyId() {
    return enemyId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public EnemyTier getTier() {
    return tier;
  }

  public String getDescription() {
    return description;
  }

  public String getImagePath() {
    return imagePath;
  }

  public int getMaxHealth() {
    return maxHealth;
  }

  public int getBaseAttack() {
    return baseAttack;
  }

  public int getArmour() {
    return armour;
  }

  public BestiaryUnlockState getUnlockState() {
    return unlockState;
  }

  /**
   * @return whether the enemy's information may be displayed
   */
  public boolean isUnlocked() {
    return unlockState != BestiaryUnlockState.LOCKED;
  }
}

package com.csse3200.game.bestiary;

import com.csse3200.game.entities.configs.EnemyTier;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Read-only, progress-aware Bestiary data intended for presentation code.
 *
 * <p>Fields that have not been unlocked are represented by empty optionals. This prevents a UI
 * implementation from accidentally revealing hidden combat details.
 *
 * @param enemyId stable enemy identifier
 * @param tier enemy difficulty tier
 * @param unlockState current discovery state
 * @param displayName visible name, or {@code ???} while locked
 * @param sprite visible sprite path after the enemy is encountered
 * @param description visible description after the enemy is defeated
 * @param health visible base health after the enemy is defeated
 * @param baseAttack visible base attack after the enemy is defeated
 * @param armour visible base armour after the enemy is defeated
 * @param behaviour visible behaviour identifier after the enemy is defeated
 */
public record BestiaryEntryView(
    String enemyId,
    EnemyTier tier,
    BestiaryUnlockState unlockState,
    String displayName,
    Optional<String> sprite,
    Optional<String> description,
    OptionalInt health,
    OptionalInt baseAttack,
    OptionalInt armour,
    Optional<String> behaviour) {
  static final String LOCKED_DISPLAY_NAME = "???";

  /** Creates a non-null, immutable view value. */
  public BestiaryEntryView {
    Objects.requireNonNull(enemyId, "enemyId");
    Objects.requireNonNull(tier, "tier");
    Objects.requireNonNull(unlockState, "unlockState");
    Objects.requireNonNull(displayName, "displayName");
    Objects.requireNonNull(sprite, "sprite");
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(health, "health");
    Objects.requireNonNull(baseAttack, "baseAttack");
    Objects.requireNonNull(armour, "armour");
    Objects.requireNonNull(behaviour, "behaviour");
  }

  static BestiaryEntryView from(BestiaryEntry entry, BestiaryUnlockState state) {
    boolean encountered = state.isAtLeast(BestiaryUnlockState.ENCOUNTERED);
    boolean defeated = state.isAtLeast(BestiaryUnlockState.DEFEATED);

    return new BestiaryEntryView(
        entry.enemyId(),
        entry.tier(),
        state,
        encountered ? entry.name() : LOCKED_DISPLAY_NAME,
        encountered ? optionalText(entry.sprite()) : Optional.empty(),
        defeated ? optionalText(entry.description()) : Optional.empty(),
        defeated ? OptionalInt.of(entry.health()) : OptionalInt.empty(),
        defeated ? OptionalInt.of(entry.baseAttack()) : OptionalInt.empty(),
        defeated ? OptionalInt.of(entry.armour()) : OptionalInt.empty(),
        defeated ? optionalText(entry.behaviour()) : Optional.empty());
  }

  /**
   * @return true when complete combat information is available
   */
  public boolean hasFullDetails() {
    return unlockState == BestiaryUnlockState.DEFEATED;
  }

  private static Optional<String> optionalText(String value) {
    return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
  }
}

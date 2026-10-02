package com.csse3200.game.components.battle;

import com.csse3200.game.entities.configs.EncounterConfig;
import com.csse3200.game.entities.configs.EncounterConfigs;
import com.csse3200.game.maps.RoomType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chooses the enemy line-up for a battle from the encounter table.
 *
 * <p>Selection is deterministic: the same room type, progression and seed always produce the same
 * line-up, so reloading a save never changes the enemies on a node.
 */
public final class EncounterComposer {
  /** Line-up used when the table has nothing suitable. Matches the pre-Sprint-3 battle. */
  public static final List<String> DEFAULT_ENEMIES = List.of("bone_crawler", "lesser_shade");

  private static final Logger logger = LoggerFactory.getLogger(EncounterComposer.class);
  private final EncounterConfigs table;
  private final Predicate<String> isKnownEnemy;

  /**
   * Creates a composer over the given encounter table.
   *
   * @param table the loaded encounter table, or null if it failed to load
   * @param isKnownEnemy returns true for enemy ids that exist in the enemy roster
   */
  public EncounterComposer(EncounterConfigs table, Predicate<String> isKnownEnemy) {
    this.table = table;
    this.isKnownEnemy = Objects.requireNonNull(isKnownEnemy, "isKnownEnemy");
  }

  /**
   * Chooses the enemies for a battle.
   *
   * @param type room type of the node being entered
   * @param progression map progression of the node being entered
   * @param seed seed that fixes the choice for this node
   * @return enemy ids in spawn order, never empty
   */
  public List<String> compose(RoomType type, int progression, long seed) {
    if (table == null) {
      logger.warn("No encounter table loaded; using the default line-up");
      return DEFAULT_ENEMIES;
    }
    List<EncounterConfig> candidates = usableCandidates(type, progression);
    if (candidates.isEmpty()) {
      logger.warn(
          "No usable encounter for {} at progression {}; using the default line-up",
          type,
          progression);
      return DEFAULT_ENEMIES;
    }
    return List.of(pickWeighted(candidates, new Random(mix(seed))).enemies);
  }

  private List<EncounterConfig> usableCandidates(RoomType type, int progression) {
    List<EncounterConfig> usable = new ArrayList<>();
    for (EncounterConfig candidate : table.matching(type, progression)) {
      if (referencesOnlyKnownEnemies(candidate)) {
        usable.add(candidate);
      }
    }
    return usable;
  }

  private boolean referencesOnlyKnownEnemies(EncounterConfig candidate) {
    for (String enemyId : candidate.enemies) {
      if (!isKnownEnemy.test(enemyId)) {
        logger.warn(
            "Encounter '{}' references unknown enemy '{}'; skipping it", candidate.id, enemyId);
        return false;
      }
    }
    return true;
  }

  /**
   * Picks one candidate, with each candidate's chance proportional to its weight.
   *
   * <p>Relies on every weight being positive, which {@link EncounterConfigs} guarantees.
   *
   * @param candidates non-empty list of candidates, in a stable order
   * @param random random source seeded for this node
   * @return the chosen candidate
   */
  private static EncounterConfig pickWeighted(List<EncounterConfig> candidates, Random random) {
    int totalWeight = 0;
    for (EncounterConfig candidate : candidates) {
      totalWeight += candidate.weight;
    }
    int roll = random.nextInt(totalWeight);
    for (EncounterConfig candidate : candidates) {
      roll -= candidate.weight;
      if (roll < 0) {
        return candidate;
      }
    }
    return candidates.get(candidates.size() - 1);
  }

  /**
   * Scrambles a seed so that neighbouring seeds lead to unrelated choices.
   *
   * <p>{@link Random} produces correlated first values for consecutive seeds, which becomes a
   * repeated pick whenever the total weight is a power of two. Node ids are consecutive, so without
   * this every node on a floor could get the same encounter. This is the SplitMix64 finaliser.
   *
   * @param seed the raw seed
   * @return a well-mixed seed
   */
  private static long mix(long seed) {
    long z = seed + 0x9E3779B97F4A7C15L;
    z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
    z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
    return z ^ (z >>> 31);
  }
}

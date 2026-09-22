package com.csse3200.game.entities.configs;

import com.csse3200.game.maps.RoomType;

/**
 * One hand-authored enemy line-up, loaded from {@code configs/encounters.json}.
 *
 * <p>Field names must match the JSON keys exactly, because libGDX binds them by reflection.
 */
public class EncounterConfig {
  public String id = "unknown";

  /** The map room type this encounter can appear in. */
  public RoomType roomType = RoomType.COMBAT;

  /** Lowest map progression (inclusive) at which this encounter can appear. */
  public int minProgression = 0;

  /**
   * Highest map progression (inclusive) at which this encounter can appear. Omitting it in JSON
   * leaves the range open-ended.
   */
  public int maxProgression = Integer.MAX_VALUE;

  /** Relative likelihood of being chosen among the other matching encounters. */
  public int weight = 1;

  /** Enemy ids from {@code configs/enemies.json}, in spawn order. Duplicates are allowed. */
  public String[] enemies = new String[0];

  /**
   * Checks whether this encounter can appear in the given room at the given progression.
   *
   * @param type room type of the node being entered
   * @param progression map progression of the node being entered
   * @return true if the room type matches and the progression is within range
   */
  public boolean matches(RoomType type, int progression) {
    return roomType == type && progression >= minProgression && progression <= maxProgression;
  }
}

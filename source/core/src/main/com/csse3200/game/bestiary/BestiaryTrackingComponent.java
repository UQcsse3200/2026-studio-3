package com.csse3200.game.bestiary;

import com.csse3200.game.components.Component;

/** Connects one enemy entity's lifecycle events to the persistent Bestiary service. */
public class BestiaryTrackingComponent extends Component {
  private final String enemyId;
  private final BestiaryService bestiary;

  /**
   * Creates a tracker for one registered enemy.
   *
   * @param enemyId stable enemy identifier
   * @param bestiary service that owns discovery progress
   */
  public BestiaryTrackingComponent(String enemyId, BestiaryService bestiary) {
    if (enemyId == null || enemyId.isBlank()) {
      throw new IllegalArgumentException("enemyId must not be null or blank");
    }
    if (bestiary == null) {
      throw new IllegalArgumentException("bestiary must not be null");
    }
    this.enemyId = enemyId;
    this.bestiary = bestiary;
  }

  /** Marks the enemy encountered and begins listening for its defeat event. */
  @Override
  public void create() {
    bestiary.recordEncountered(enemyId);
    entity.getEvents().addListener("enemyDefeated", this::onEnemyDefeated);
  }

  /**
   * @return stable ID of the tracked enemy
   */
  public String getEnemyId() {
    return enemyId;
  }

  private void onEnemyDefeated() {
    bestiary.recordDefeated(enemyId);
  }
}

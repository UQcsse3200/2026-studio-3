package com.csse3200.game.encounters.integration;

/** Ownership-only boundary for transactional Chance item rewards; never applies item effects. */
public interface ItemRewardGateway {
  /** Returns whether the authoritative item catalogue defines this ID. Does not mutate state. */
  boolean containsItem(String itemId);

  /**
   * Attempts to append one owned copy and retains rollback metadata before mutation.
   *
   * <p>The caller must roll back the attempt even when this returns false or throws: a dependency
   * may fail after changing ownership.
   *
   * @param itemId validated item ID
   * @return true when exactly one copy was added
   */
  boolean addItem(String itemId);

  /**
   * Discards rollback metadata for the most recent matching successful addition, without effects.
   */
  void commitItemAddition(String itemId);

  /**
   * Reverts the most recent matching attempt without removing pre-existing ownership.
   *
   * @param itemId attempted item ID
   * @return true when the original ownership, including order, was restored
   */
  boolean rollbackItemAddition(String itemId);
}

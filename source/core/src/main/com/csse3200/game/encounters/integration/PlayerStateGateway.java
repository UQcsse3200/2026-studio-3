package com.csse3200.game.encounters.integration;

/**
 * Minimal player-state contract required by non-battle encounters.
 *
 * <p>The encounter system depends on this interface instead of Team 7 implementation classes. A
 * small adapter can therefore absorb later Player API changes without changing Chance or Shop
 * logic.
 */
public interface PlayerStateGateway {
  /**
   * Returns the player's current health.
   *
   * @return the player's current health
   */
  int getHealth();

  /**
   * Updates the player's health. The concrete Player system owns its health bounds.
   *
   * @param health requested health value
   */
  void setHealth(int health);

  /**
   * Returns the player's current spendable currency.
   *
   * @return the player's current spendable currency
   */
  int getCurrency();

  /**
   * Updates the player's currency.
   *
   * @param currency non-negative currency value
   */
  void setCurrency(int currency);
}

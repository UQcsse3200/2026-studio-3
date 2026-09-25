package com.csse3200.game.components.enemy.EnemyAI;

import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.PlayerMemory;
import java.util.Objects;

/** Read-only battle information used by an enemy AI to select its next intent. */
public class EnemyAIContext {
  private final int playerHealth;
  private final int enemyHealth;
  private final int enemyMaxHealth;
  private final int enemyAttack;
  private final int enemyArmour;
  private final EnemyIntent previousIntent;
  private final int turnNumber;
  private final PlayerMemory playerMemory;

  /**
   * Creates a battle-state snapshot without player behaviour history.
   *
   * <p>This constructor is retained for backwards compatibility. Existing enemy AI classes and
   * tests can continue using the original seven arguments. The context receives an empty player
   * memory instead of {@code null}.
   *
   * @param playerHealth player's current health
   * @param enemyHealth enemy's current health
   * @param enemyMaxHealth enemy's maximum health
   * @param enemyAttack enemy's current base attack
   * @param enemyArmour enemy's current armour
   * @param previousIntent enemy's intent from the previous turn
   * @param turnNumber current battle turn number
   */
  public EnemyAIContext(
      int playerHealth,
      int enemyHealth,
      int enemyMaxHealth,
      int enemyAttack,
      int enemyArmour,
      EnemyIntent previousIntent,
      int turnNumber) {
    this(
        playerHealth,
        enemyHealth,
        enemyMaxHealth,
        enemyAttack,
        enemyArmour,
        previousIntent,
        turnNumber,
        PlayerMemory.empty());
  }

  /**
   * Creates a complete battle-state snapshot including the player's observed combat behaviour.
   *
   * @param playerHealth player's current health
   * @param enemyHealth enemy's current health
   * @param enemyMaxHealth enemy's maximum health
   * @param enemyAttack enemy's current base attack
   * @param enemyArmour enemy's current armour
   * @param previousIntent enemy's intent from the previous turn
   * @param turnNumber current battle turn number
   * @param playerMemory immutable snapshot of the player's observed combat behaviour
   */
  public EnemyAIContext(
      int playerHealth,
      int enemyHealth,
      int enemyMaxHealth,
      int enemyAttack,
      int enemyArmour,
      EnemyIntent previousIntent,
      int turnNumber,
      PlayerMemory playerMemory) {
    this.playerHealth = Math.max(0, playerHealth);
    this.enemyHealth = Math.max(0, enemyHealth);
    this.enemyMaxHealth = Math.max(0, enemyMaxHealth);
    this.enemyAttack = Math.max(0, enemyAttack);
    this.enemyArmour = Math.max(0, enemyArmour);
    this.previousIntent = Objects.requireNonNull(previousIntent, "previousIntent cannot be null");
    this.turnNumber = Math.max(1, turnNumber);
    this.playerMemory = Objects.requireNonNull(playerMemory, "playerMemory cannot be null");
  }

  public int getPlayerHealth() {
    return playerHealth;
  }

  public int getEnemyHealth() {
    return enemyHealth;
  }

  public int getEnemyMaxHealth() {
    return enemyMaxHealth;
  }

  public int getEnemyAttack() {
    return enemyAttack;
  }

  public int getEnemyArmour() {
    return enemyArmour;
  }

  public EnemyIntent getPreviousIntent() {
    return previousIntent;
  }

  public int getTurnNumber() {
    return turnNumber;
  }

  /**
   * Returns an immutable snapshot of the player's observed combat behaviour.
   *
   * @return player behaviour memory, never {@code null}
   */
  public PlayerMemory getPlayerMemory() {
    return playerMemory;
  }

  /**
   * Returns the enemy's health as a value between 0 and 1.
   *
   * @return current health divided by maximum health
   */
  public float getEnemyHealthRatio() {
    if (enemyMaxHealth <= 0) {
      return 0f;
    }

    return (float) enemyHealth / enemyMaxHealth;
  }

  /**
   * Checks whether a normal attack can defeat the player.
   *
   * @return true when the enemy's current attack is at least the player's health
   */
  public boolean canDefeatPlayer() {
    return enemyAttack >= playerHealth;
  }
}

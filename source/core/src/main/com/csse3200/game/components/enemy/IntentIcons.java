package com.csse3200.game.components.enemy;

/**
 * Maps {@link IntentType} values to their HUD icon textures.
 *
 * <p>The textures live under {@code images/enemies/intents/} and are queued for loading by {@code
 * EnemyFactory.loadAssets()}.
 */
public final class IntentIcons {
  private static final String DIR = "images/enemies/intents/";

  public static final String ATTACK = DIR + "attack.png";
  public static final String DEFEND = DIR + "defend.png";
  public static final String BUFF = DIR + "buff.png";
  public static final String DEBUFF = DIR + "debuff.png";
  public static final String UNKNOWN = DIR + "unknown.png";

  private static final String[] ALL = {ATTACK, DEFEND, BUFF, DEBUFF, UNKNOWN};

  /**
   * @return every intent icon texture path
   */
  public static String[] all() {
    return ALL.clone();
  }

  /**
   * Returns the icon texture path for an intent type.
   *
   * @param type the intent type
   * @return an internal texture path
   */
  public static String pathFor(IntentType type) {
    return switch (type) {
      case ATTACK -> ATTACK;
      case DEFEND -> DEFEND;
      case BUFF -> BUFF;
      case DEBUFF -> DEBUFF;
      case UNKNOWN -> UNKNOWN;
    };
  }

  private IntentIcons() {
    throw new IllegalStateException("Instantiating utility class");
  }
}

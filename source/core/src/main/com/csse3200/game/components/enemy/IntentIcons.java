package com.csse3200.game.components.enemy;

/**
 * Maps enemy intents to their HUD icon textures.
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

  // Status-specific debuff icons.
  public static final String SILENCE = DIR + "silence.png";
  public static final String DAMAGE_ON_CARD_PLAY = DIR + "damage_on_card_play.png";
  public static final String TAUNT = DIR + "taunt.png";

  /*
   * EnemyFactory.loadAssets() uses this array to preload every intent icon.
   * Any newly added icon must also be added here.
   */
  private static final String[] ALL = {
    ATTACK, DEFEND, BUFF, DEBUFF, UNKNOWN, SILENCE, DAMAGE_ON_CARD_PLAY, TAUNT
  };

  /**
   * Returns every intent icon texture path.
   *
   * @return a copy of all intent icon paths
   */
  public static String[] all() {
    return ALL.clone();
  }

  /**
   * Returns the icon texture path for an intent type.
   *
   * <p>This method is retained for backward compatibility. Debuffs queried through this method use
   * the generic debuff icon.
   *
   * @param type the intent type
   * @return an internal texture path
   */
  public static String pathFor(IntentType type) {
    if (type == null) {
      return UNKNOWN;
    }

    return switch (type) {
      case ATTACK -> ATTACK;
      case DEFEND -> DEFEND;
      case BUFF -> BUFF;
      case DEBUFF -> DEBUFF;
      case UNKNOWN -> UNKNOWN;
    };
  }

  /**
   * Returns the appropriate icon for an intent and its specific status effect.
   *
   * <p>Only debuff intents are split by effect type. All other intent types use the existing
   * type-based icon mapping.
   *
   * @param type the general intent type
   * @param effectType the status effect applied by the intent; may be null
   * @return an internal texture path
   */
  public static String pathFor(IntentType type, IntentEffectType effectType) {
    if (type != IntentType.DEBUFF || effectType == null) {
      return pathFor(type);
    }

    return switch (effectType) {
      case SILENCE -> SILENCE;
      case DAMAGE_ON_CARD_PLAY -> DAMAGE_ON_CARD_PLAY;
    };
  }

  private IntentIcons() {
    throw new IllegalStateException("Instantiating utility class");
  }
}

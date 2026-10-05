package com.csse3200.game.tutorial;

/**
 * Short battle teaching text for the guided tutorial to display at its chosen steps.
 *
 * <p>This class supplies content only. It does not define step order, progression, highlights, or
 * when a prompt is shown.
 */
public final class BattleTutorialPromptContent {
  /** Introduces the opening hand without assuming a full five cards are always available. */
  public static final String OPENING_HAND =
      "You draw up to five cards at battle start. These are your hand; choose one to play.";

  /** Explains the cost displayed on a playable card. */
  public static final String CARD_COST =
      "A card's cost is the energy needed to play it. Choose a card you can afford.";

  /** Explains spending and start-of-turn replenishment. */
  public static final String ENERGY =
      "Playing cards spends energy. Your energy refills at the start of each of your turns.";

  /** Explains the player's defeat condition. */
  public static final String HEALTH = "Keep your health above zero to stay in the fight.";

  /** Introduces combat modifiers without assuming a particular effect is present. */
  public static final String BUFFS_AND_STATUS =
      "Buffs and status effects can change damage or apply ongoing effects.";

  /** Explains drawing from the temporary battle deck. */
  public static final String CARD_DRAW =
      "Drawing moves a card from your battle deck into your hand.";

  /** Explains how the player yields control in an ongoing battle. */
  public static final String END_TURN =
      "Press End Turn when ready. Surviving enemies act before your next turn.";

  /** Explains both terminal battle outcomes. */
  public static final String WIN_OR_LOSE =
      "Defeat every enemy to win. If your health reaches zero, you lose.";

  private BattleTutorialPromptContent() {}
}

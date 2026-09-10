package com.csse3200.game.cards.deck;

import java.util.Set;

/**
 * Registry of card IDs that may be stored in a player deck.
 *
 * <p>This registry contains the card IDs shared with the Shop system. When Team 6's card library is
 * available, registration checks can delegate to that library without changing the {@link
 * PlayerDeck} API.
 */
public final class CardIdRegistry {
  public static final String STRIKE = "strike";
  public static final String DEFEND = "defend";
  public static final String POISON_DAGGER = "poison_dagger";
  public static final String EXPOSE = "expose";
  public static final String INNER_FOCUS = "inner_focus";
  public static final String BANDAGE = "bandage";
  public static final String STARFALL = "starfall";
  public static final String RIFT_LANCE = "rift_lance";
  public static final String ASTRAL_WARD = "astral_ward";
  public static final String RESURRECTION = "resurrection";

  private static final Set<String> REGISTERED_CARD_IDS =
      Set.of(
          STRIKE,
          DEFEND,
          POISON_DAGGER,
          EXPOSE,
          INNER_FOCUS,
          BANDAGE,
          STARFALL,
          RIFT_LANCE,
          ASTRAL_WARD,
          RESURRECTION);

  private CardIdRegistry() {
    throw new IllegalStateException("Instantiating utility class");
  }

  /**
   * Checks whether a card ID is registered for use by card and deck systems.
   *
   * @param cardId card ID to check
   * @return true when the ID is non-blank and registered, otherwise false
   */
  public static boolean isRegistered(String cardId) {
    return cardId != null && !cardId.isBlank() && REGISTERED_CARD_IDS.contains(cardId);
  }
}

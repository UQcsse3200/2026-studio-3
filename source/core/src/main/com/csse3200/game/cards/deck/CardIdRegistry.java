package com.csse3200.game.cards.deck;

import java.util.Set;

/**
 * Registry of card IDs that may be stored in a player deck.
 *
 * <p>This temporary Sprint 1 registry contains the six card IDs shared with the Shop system. When
 * Team 6's card library is available, registration checks can delegate to that library without
 * changing the {@link PlayerDeck} API.
 */
public final class CardIdRegistry {
  public static final String STRIKE = "strike";
  public static final String DEFEND = "defend";
  public static final String POISON_DAGGER = "poison_dagger";
  public static final String EXPOSE = "expose";
  public static final String INNER_FOCUS = "inner_focus";
  public static final String BANDAGE = "bandage";
  public static final String SEALED_PACT = "sealed_pact";
  public static final String BLOOD_PRICE = "blood_price";
  public static final String DOOM_SIGIL = "doom_sigil";
  public static final String ECLIPSE_DECREE = "eclipse_decree";
  public static final String IRON_OATH = "iron_oath";

  private static final Set<String> REGISTERED_CARD_IDS =
      Set.of(
          STRIKE,
          DEFEND,
          POISON_DAGGER,
          EXPOSE,
          INNER_FOCUS,
          BANDAGE,
          SEALED_PACT,
          BLOOD_PRICE,
          DOOM_SIGIL,
          ECLIPSE_DECREE,
          IRON_OATH);

  private CardIdRegistry() {
    throw new IllegalStateException("Instantiating utility class");
  }

  /**
   * Checks whether a card ID is registered for use by Sprint 1 systems.
   *
   * @param cardId card ID to check
   * @return true when the ID is non-blank and registered, otherwise false
   */
  public static boolean isRegistered(String cardId) {
    return cardId != null && !cardId.isBlank() && REGISTERED_CARD_IDS.contains(cardId);
  }
}

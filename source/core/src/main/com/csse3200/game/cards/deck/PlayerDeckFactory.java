package com.csse3200.game.cards.deck;

import com.csse3200.game.cards.CardService;
import java.util.List;

/** Creates standard player decks from the initial Team 6 card IDs. */
public final class PlayerDeckFactory {
  public static final String STRIKE = "strike";
  public static final String DEFEND = "defend";
  public static final String POISON_DAGGER = "poison_dagger";
  public static final String EXPOSE = "expose";
  public static final String INNER_FOCUS = "inner_focus";
  public static final String BANDAGE = "bandage";

  private static final List<String> STARTER_DECK_CARD_IDS =
      List.of(
          STRIKE,
          STRIKE,
          STRIKE,
          DEFEND,
          DEFEND,
          DEFEND,
          POISON_DAGGER,
          EXPOSE,
          BANDAGE,
          INNER_FOCUS);

  private PlayerDeckFactory() {
    throw new IllegalStateException("Instantiating utility class");
  }

  /**
   * Creates a default player deck using Team 6's six initial cards.
   *
   * <p>This is intentionally separate from battle deck state. Combat systems should copy this deck
   * when a battle begins, then shuffle and mutate their own draw pile, hand and discard pile.
   *
   * @return starter player deck
   */
  public static PlayerDeck createStarterDeck() {
    return new PlayerDeck(STARTER_DECK_CARD_IDS);
  }

  /**
   * Creates a default player deck validated by the supplied card service.
   *
   * @param cardService authoritative card lookup service
   * @return starter player deck
   */
  public static PlayerDeck createStarterDeck(CardService cardService) {
    return new PlayerDeck(cardService, STARTER_DECK_CARD_IDS);
  }

  /**
   * Returns the card IDs used by the starter deck.
   *
   * @return immutable starter deck card IDs
   */
  public static List<String> getStarterDeckCardIds() {
    return STARTER_DECK_CARD_IDS;
  }
}

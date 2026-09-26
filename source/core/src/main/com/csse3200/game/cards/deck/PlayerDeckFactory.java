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
  public static final String SEALED_PACT = "sealed_pact";
  public static final String BLOOD_PRICE = "blood_price";
  public static final String DOOM_SIGIL = "doom_sigil";
  public static final String IRON_OATH = "iron_oath";

  private static final List<String> STARTER_DECK_CARD_IDS =
      List.of(
          STRIKE,
          STRIKE,
          STRIKE,
          STRIKE, // TEMP: 11th card so the deck-rearrange popup's pagination has a second page to
          // show. Remove this line (and the matching count bump in PlayerDeckFactoryTest) once
          // pagination has been checked.
          DEFEND,
          DEFEND,
          DEFEND,
          POISON_DAGGER,
          EXPOSE,
          BANDAGE,
          INNER_FOCUS);

  private static final List<String> FORBIDDEN_TEST_DECK_CARD_IDS =
      List.of(
          SEALED_PACT,
          BLOOD_PRICE,
          BLOOD_PRICE,
          DOOM_SIGIL,
          DOOM_SIGIL,
          IRON_OATH,
          STRIKE,
          STRIKE,
          DEFEND,
          DEFEND);

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

  /**
   * Creates a deterministic deck for verifying the Round 2 forbidden cards.
   *
   * <p>This is a test/demo entry point only. The default starter deck is intentionally unchanged;
   * production acquisition remains owned by the reward and shop integrations.
   *
   * @return player deck containing the four Member 5 cards and supporting initial cards
   */
  public static PlayerDeck createForbiddenTestDeck() {
    return new PlayerDeck(FORBIDDEN_TEST_DECK_CARD_IDS);
  }

  /**
   * Creates a deterministic forbidden-card test deck validated by the supplied card service.
   *
   * @param cardService authoritative card lookup service
   * @return player deck containing the four Member 5 cards and supporting initial cards
   */
  public static PlayerDeck createForbiddenTestDeck(CardService cardService) {
    return new PlayerDeck(cardService, FORBIDDEN_TEST_DECK_CARD_IDS);
  }

  /**
   * Returns the card IDs used by the forbidden-card test deck.
   *
   * @return immutable test-deck card IDs
   */
  public static List<String> getForbiddenTestDeckCardIds() {
    return FORBIDDEN_TEST_DECK_CARD_IDS;
  }
}

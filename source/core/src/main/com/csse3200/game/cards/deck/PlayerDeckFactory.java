package com.csse3200.game.cards.deck;

import java.util.List;

/** Creates standard player decks from the initial Team 6 card IDs. */
public final class PlayerDeckFactory {
  public static final String STRIKE = CardIdRegistry.STRIKE;
  public static final String DEFEND = CardIdRegistry.DEFEND;
  public static final String POISON_DAGGER = CardIdRegistry.POISON_DAGGER;
  public static final String EXPOSE = CardIdRegistry.EXPOSE;
  public static final String INNER_FOCUS = CardIdRegistry.INNER_FOCUS;
  public static final String BANDAGE = CardIdRegistry.BANDAGE;
  public static final String SEALED_PACT = CardIdRegistry.SEALED_PACT;
  public static final String BLOOD_PRICE = CardIdRegistry.BLOOD_PRICE;
  public static final String DOOM_SIGIL = CardIdRegistry.DOOM_SIGIL;
  public static final String ECLIPSE_DECREE = CardIdRegistry.ECLIPSE_DECREE;

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

  private static final List<String> FORBIDDEN_TEST_DECK_CARD_IDS =
      List.of(
          SEALED_PACT,
          BLOOD_PRICE,
          BLOOD_PRICE,
          DOOM_SIGIL,
          DOOM_SIGIL,
          ECLIPSE_DECREE,
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
   * @return player deck containing all four forbidden cards and supporting initial cards
   */
  public static PlayerDeck createForbiddenTestDeck() {
    return new PlayerDeck(FORBIDDEN_TEST_DECK_CARD_IDS);
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

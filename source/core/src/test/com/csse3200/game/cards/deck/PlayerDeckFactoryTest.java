package com.csse3200.game.cards.deck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.TestCardService;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlayerDeckFactoryTest {
  private static final CardService CARDS =
      TestCardService.withCards(
          "strike", "defend", "poison_dagger", "expose", "inner_focus", "bandage");

  @Test
  void shouldCreateStarterDeckFromTeamSixCards() {
    PlayerDeck deck = PlayerDeckFactory.createStarterDeck(CARDS);

    assertEquals(10, deck.size());
    assertEquals(3, deck.count(PlayerDeckFactory.STRIKE));
    assertEquals(3, deck.count(PlayerDeckFactory.DEFEND));
    assertEquals(1, deck.count(PlayerDeckFactory.POISON_DAGGER));
    assertEquals(1, deck.count(PlayerDeckFactory.EXPOSE));
    assertEquals(1, deck.count(PlayerDeckFactory.BANDAGE));
    assertEquals(1, deck.count(PlayerDeckFactory.INNER_FOCUS));
  }

  @Test
  void shouldIncludeAllInitialTeamSixCardIds() {
    List<String> cardIds = PlayerDeckFactory.getStarterDeckCardIds();

    assertTrue(cardIds.contains(PlayerDeckFactory.STRIKE));
    assertTrue(cardIds.contains(PlayerDeckFactory.DEFEND));
    assertTrue(cardIds.contains(PlayerDeckFactory.POISON_DAGGER));
    assertTrue(cardIds.contains(PlayerDeckFactory.EXPOSE));
    assertTrue(cardIds.contains(PlayerDeckFactory.INNER_FOCUS));
    assertTrue(cardIds.contains(PlayerDeckFactory.BANDAGE));
  }

  @Test
  void shouldReturnImmutableStarterDeckIds() {
    List<String> cardIds = PlayerDeckFactory.getStarterDeckCardIds();

    assertThrows(UnsupportedOperationException.class, () -> cardIds.add(PlayerDeckFactory.STRIKE));
  }

  @Test
  void shouldCreateIndependentStarterDecks() {
    PlayerDeck first = PlayerDeckFactory.createStarterDeck(CARDS);
    PlayerDeck second = PlayerDeckFactory.createStarterDeck(CARDS);

    first.removeCard(PlayerDeckFactory.STRIKE);

    assertEquals(9, first.size());
    assertEquals(10, second.size());
    assertEquals(3, second.count(PlayerDeckFactory.STRIKE));
  }
}

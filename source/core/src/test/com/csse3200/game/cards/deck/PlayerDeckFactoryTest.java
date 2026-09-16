package com.csse3200.game.cards.deck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.TestCardService;
import com.csse3200.game.cards.runtime.CardInstance;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlayerDeckFactoryTest {
  private static final CardService CARDS =
      TestCardService.withCards(
          "strike", "defend", "poison_dagger", "expose", "inner_focus", "bandage");

  private static final CardService FORBIDDEN_CARDS =
      TestCardService.withCards(
          "strike", "defend", "sealed_pact", "blood_price", "doom_sigil", "iron_oath");

  @Test
  void shouldCreateStarterDeckFromTeamSixCards() {
    PlayerDeck deck = PlayerDeckFactory.createStarterDeck(CARDS);

    // TEMP: starter deck has an extra STRIKE (11 cards, 4 of them) while pagination is being
    // checked — see the TEMP comment in PlayerDeckFactory. Revert both together afterward.
    assertEquals(11, deck.size());
    assertEquals(4, deck.countByCardId(PlayerDeckFactory.STRIKE));
    assertEquals(3, deck.countByCardId(PlayerDeckFactory.DEFEND));
    assertEquals(1, deck.countByCardId(PlayerDeckFactory.POISON_DAGGER));
    assertEquals(1, deck.countByCardId(PlayerDeckFactory.EXPOSE));
    assertEquals(1, deck.countByCardId(PlayerDeckFactory.BANDAGE));
    assertEquals(1, deck.countByCardId(PlayerDeckFactory.INNER_FOCUS));
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

    assertEquals(10, first.size());
    assertEquals(11, second.size());
    assertEquals(4, second.countByCardId(PlayerDeckFactory.STRIKE));
  }

  @Test
  void shouldCreateDistinctBaseInstancesInStarterOrder() {
    PlayerDeck first = PlayerDeckFactory.createStarterDeck(CARDS);
    PlayerDeck second = PlayerDeckFactory.createStarterDeck(CARDS);
    assertEquals(
        PlayerDeckFactory.getStarterDeckCardIds(),
        first.getCards().stream().map(CardInstance::cardId).toList());
    assertEquals(
        first.size(), first.getCards().stream().map(CardInstance::instanceId).distinct().count());
    assertTrue(first.getCards().stream().allMatch(card -> !card.isUpgraded()));
    assertTrue(first.getCards().stream().noneMatch(card -> second.getCards().contains(card)));
  }

  @Test
  void shouldCreateForbiddenTestDeck() {
    PlayerDeck deck = PlayerDeckFactory.createForbiddenTestDeck(FORBIDDEN_CARDS);

    assertEquals(10, deck.size());
    assertEquals(1, deck.countByCardId(PlayerDeckFactory.SEALED_PACT));
    assertEquals(2, deck.countByCardId(PlayerDeckFactory.BLOOD_PRICE));
    assertEquals(2, deck.countByCardId(PlayerDeckFactory.DOOM_SIGIL));
    assertEquals(1, deck.countByCardId(PlayerDeckFactory.IRON_OATH));
  }

  @Test
  void shouldIncludeAllForbiddenCardIdsInTestDeck() {
    List<String> cardIds = PlayerDeckFactory.getForbiddenTestDeckCardIds();

    assertTrue(cardIds.contains(PlayerDeckFactory.SEALED_PACT));
    assertTrue(cardIds.contains(PlayerDeckFactory.BLOOD_PRICE));
    assertTrue(cardIds.contains(PlayerDeckFactory.DOOM_SIGIL));
    assertTrue(cardIds.contains(PlayerDeckFactory.IRON_OATH));
  }

  @Test
  void shouldReturnImmutableForbiddenTestDeckIds() {
    List<String> cardIds = PlayerDeckFactory.getForbiddenTestDeckCardIds();

    assertThrows(
        UnsupportedOperationException.class, () -> cardIds.add(PlayerDeckFactory.SEALED_PACT));
  }
}

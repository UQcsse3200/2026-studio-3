package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.TestCardService;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.runtime.CardInstance;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlayerDeckAdapterTest {
  @Test
  void shouldCreateNewInstanceWhenAddingByCardId() {
    PlayerDeck deck = deck("strike");
    PlayerDeckAdapter adapter = new PlayerDeckAdapter(deck);
    List<CardInstance> before = deck.getCards();

    assertTrue(adapter.addCard("strike"));

    List<CardInstance> after = deck.getCards();
    assertEquals(2, after.size());
    assertEquals("strike", after.get(1).cardId());
    assertNotEquals(before.get(0).instanceId(), after.get(1).instanceId());
    assertEquals(0, after.get(1).upgradeLevel());
  }

  @Test
  void shouldRollbackOnlyThePendingInstanceWhenDuplicatesExist() {
    PlayerDeck deck = deck("strike", "defend", "strike");
    PlayerDeckAdapter adapter = new PlayerDeckAdapter(deck);
    List<CardInstance> before = deck.getCards();

    assertTrue(adapter.addCard("strike"));
    assertEquals(4, deck.size());

    assertTrue(adapter.rollbackCardAddition("strike"));
    assertEquals(before, deck.getCards());
  }

  @Test
  void shouldRejectRollbackWhenCardIdDoesNotMatchWithoutMutation() {
    PlayerDeck deck = deck("strike", "defend");
    PlayerDeckAdapter adapter = new PlayerDeckAdapter(deck);
    List<CardInstance> before = deck.getCards();

    assertTrue(adapter.addCard("strike"));
    assertFalse(adapter.rollbackCardAddition(null));
    assertFalse(adapter.rollbackCardAddition("defend"));

    assertEquals(List.of(before.get(0), before.get(1), deck.getCards().get(2)), deck.getCards());
  }

  @Test
  void shouldRemoveOneMatchingCopyByCardIdForLegacyDeckGatewayRemove() {
    PlayerDeck deck = deck("strike", "defend", "strike");
    PlayerDeckAdapter adapter = new PlayerDeckAdapter(deck);
    List<CardInstance> before = deck.getCards();

    assertTrue(adapter.removeCard("strike"));

    assertEquals(List.of(before.get(1), before.get(2)), deck.getCards());
  }

  @Test
  void shouldRejectMissingCardsAndNullDeck() {
    PlayerDeckAdapter adapter = new PlayerDeckAdapter(deck("strike"));

    assertFalse(adapter.canAddCard("missing"));
    assertFalse(adapter.addCard("missing"));
    assertFalse(adapter.removeCard("missing"));
    assertThrows(NullPointerException.class, () -> new PlayerDeckAdapter(null));
  }

  private static PlayerDeck deck(String... cardIds) {
    return new PlayerDeck(TestCardService.withCards("strike", "defend"), List.of(cardIds));
  }
}

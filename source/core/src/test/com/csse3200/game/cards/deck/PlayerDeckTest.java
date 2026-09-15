package com.csse3200.game.cards.deck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.TestCardService;
import com.csse3200.game.cards.runtime.CardInstance;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlayerDeckTest {
  private static final CardService CARDS =
      TestCardService.withCards(
          "strike",
          "defend",
          "poison_dagger",
          "expose",
          "inner_focus",
          "bandage",
          "new_team_six_card");

  @Test
  void shouldCreateEmptyDeck() {
    PlayerDeck deck = new PlayerDeck(CARDS);

    assertTrue(deck.isEmpty());
    assertEquals(0, deck.size());
    assertTrue(deck.getCards().isEmpty());
  }

  @Test
  void shouldCreateDeckFromCardIdsInOrder() {
    PlayerDeck deck = new PlayerDeck(CARDS, List.of("strike", "defend", "bandage"));

    assertEquals(3, deck.size());
    assertIterableEquals(List.of("strike", "defend", "bandage"), cardIds(deck));
  }

  @Test
  void shouldAddAndCountDuplicateCards() {
    PlayerDeck deck = new PlayerDeck(CARDS);

    deck.addCard(new CardInstance("strike-1", "strike", 0));
    deck.addCard(new CardInstance("strike-2", "strike", 0));
    deck.addCard(new CardInstance("defend-1", "defend", 0));

    assertEquals(3, deck.size());
    assertEquals(2, deck.countByCardId("strike"));
    assertEquals(1, deck.countByCardId("defend"));
  }

  @Test
  void shouldAllowRegisteredCardsToBeAdded() {
    PlayerDeck deck = new PlayerDeck(CARDS);

    for (String cardId : PlayerDeckFactory.getStarterDeckCardIds()) {
      assertTrue(deck.canAddCard(cardId));
    }
    assertTrue(deck.canAddCard("new_team_six_card"));

    assertTrue(deck.isEmpty());
  }

  @Test
  void shouldRejectInvalidCardsBeforeAdding() {
    PlayerDeck deck = new PlayerDeck(CARDS);

    assertFalse(deck.canAddCard(null));
    assertFalse(deck.canAddCard(""));
    assertFalse(deck.canAddCard("  "));
    assertFalse(deck.canAddCard("unknown_card"));
    assertTrue(deck.isEmpty());
  }

  @Test
  void shouldRejectUnknownCardWithoutChangingDeck() {
    PlayerDeck deck = new PlayerDeck(CARDS, List.of("strike"));

    CardInstance unknown = new CardInstance("unknown-instance", "unknown_card", 0);
    assertThrows(IllegalArgumentException.class, () -> deck.addCard(unknown));

    assertIterableEquals(List.of("strike"), cardIds(deck));
  }

  @Test
  void shouldAddMultipleCardsInOrder() {
    PlayerDeck deck = new PlayerDeck(CARDS);

    deck.addCards(List.of("strike", "defend", "poison_dagger"));

    assertIterableEquals(List.of("strike", "defend", "poison_dagger"), cardIds(deck));
  }

  @Test
  void shouldRemoveSelectedInstanceOnly() {
    PlayerDeck deck = new PlayerDeck(CARDS, List.of("strike", "defend", "strike"));

    assertTrue(deck.removeCard(deck.getCards().get(0).instanceId()));

    assertIterableEquals(List.of("defend", "strike"), cardIds(deck));
    assertEquals(1, deck.countByCardId("strike"));
  }

  @Test
  void shouldReturnFalseWhenRemovingMissingCard() {
    PlayerDeck deck = new PlayerDeck(CARDS, List.of("strike", "defend"));

    assertFalse(deck.removeCard("bandage"));

    assertIterableEquals(List.of("strike", "defend"), cardIds(deck));
  }

  @Test
  void shouldRemoveCardAtPosition() {
    PlayerDeck deck = new PlayerDeck(CARDS, List.of("strike", "defend", "bandage"));

    CardInstance removed = deck.removeCardAt(1);

    assertEquals("defend", removed.cardId());
    assertIterableEquals(List.of("strike", "bandage"), cardIds(deck));
  }

  @Test
  void shouldRejectInvalidCards() {
    PlayerDeck deck = new PlayerDeck(CARDS);
    CardInstance unknown = new CardInstance("unknown-instance", "unknown_card", 0);

    assertThrows(IllegalArgumentException.class, () -> deck.addCard((CardInstance) null));
    assertThrows(IllegalArgumentException.class, () -> deck.addCard(unknown));
    assertThrows(IllegalArgumentException.class, () -> deck.addCards(null));
    assertThrows(
        IllegalArgumentException.class, () -> new PlayerDeck(CARDS, List.of("strike", "")));
  }

  @Test
  void shouldReturnImmutableSnapshot() {
    PlayerDeck deck = new PlayerDeck(CARDS, List.of("strike", "defend"));
    List<CardInstance> snapshot = deck.getCards();

    assertThrows(UnsupportedOperationException.class, snapshot::clear);

    assertIterableEquals(List.of("strike", "defend"), cardIds(deck));
  }

  @Test
  void shouldCopyIndependently() {
    PlayerDeck original = new PlayerDeck(CARDS, List.of("strike", "defend"));
    PlayerDeck copy = original.copy();

    copy.addCard(new CardInstance("bandage-copy", "bandage", 0));
    original.removeCard(original.getCards().get(0).instanceId());

    assertIterableEquals(List.of("defend"), cardIds(original));
    assertIterableEquals(List.of("strike", "defend", "bandage"), cardIds(copy));
  }

  @Test
  void shouldClearDeck() {
    PlayerDeck deck = new PlayerDeck(CARDS, List.of("strike", "defend"));

    deck.clear();

    assertTrue(deck.isEmpty());
    assertEquals(0, deck.size());
  }

  private static List<String> cardIds(PlayerDeck deck) {
    return deck.getCards().stream().map(CardInstance::cardId).toList();
  }
}

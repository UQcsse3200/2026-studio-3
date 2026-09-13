package com.csse3200.game.cards.deck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.TestCardService;
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
    assertTrue(deck.getCardIds().isEmpty());
  }

  @Test
  void shouldCreateDeckFromCardIdsInOrder() {
    PlayerDeck deck = new PlayerDeck(CARDS, List.of("strike", "defend", "bandage"));

    assertEquals(3, deck.size());
    assertIterableEquals(List.of("strike", "defend", "bandage"), deck.getCardIds());
  }

  @Test
  void shouldAddAndCountDuplicateCards() {
    PlayerDeck deck = new PlayerDeck(CARDS);

    deck.addCard("strike");
    deck.addCard("strike");
    deck.addCard("defend");

    assertEquals(3, deck.size());
    assertEquals(2, deck.count("strike"));
    assertEquals(1, deck.count("defend"));
    assertTrue(deck.contains("strike"));
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

    assertThrows(IllegalArgumentException.class, () -> deck.addCard("unknown_card"));

    assertIterableEquals(List.of("strike"), deck.getCardIds());
  }

  @Test
  void shouldAddMultipleCardsInOrder() {
    PlayerDeck deck = new PlayerDeck(CARDS);

    deck.addCards(List.of("strike", "defend", "poison_dagger"));

    assertIterableEquals(List.of("strike", "defend", "poison_dagger"), deck.getCardIds());
  }

  @Test
  void shouldRemoveFirstMatchingCardOnly() {
    PlayerDeck deck = new PlayerDeck(CARDS, List.of("strike", "defend", "strike"));

    assertTrue(deck.removeCard("strike"));

    assertIterableEquals(List.of("defend", "strike"), deck.getCardIds());
    assertEquals(1, deck.count("strike"));
  }

  @Test
  void shouldReturnFalseWhenRemovingMissingCard() {
    PlayerDeck deck = new PlayerDeck(CARDS, List.of("strike", "defend"));

    assertFalse(deck.removeCard("bandage"));

    assertIterableEquals(List.of("strike", "defend"), deck.getCardIds());
  }

  @Test
  void shouldRemoveCardAtPosition() {
    PlayerDeck deck = new PlayerDeck(CARDS, List.of("strike", "defend", "bandage"));

    String removed = deck.removeCardAt(1);

    assertEquals("defend", removed);
    assertIterableEquals(List.of("strike", "bandage"), deck.getCardIds());
  }

  @Test
  void shouldRejectInvalidCardIds() {
    PlayerDeck deck = new PlayerDeck(CARDS);

    assertThrows(IllegalArgumentException.class, () -> deck.addCard(null));
    assertThrows(IllegalArgumentException.class, () -> deck.addCard(""));
    assertThrows(IllegalArgumentException.class, () -> deck.addCard("  "));
    assertThrows(IllegalArgumentException.class, () -> deck.addCard("unknown_card"));
    assertThrows(IllegalArgumentException.class, () -> deck.addCards(null));
    assertThrows(
        IllegalArgumentException.class, () -> new PlayerDeck(CARDS, List.of("strike", "")));
  }

  @Test
  void shouldReturnImmutableSnapshot() {
    PlayerDeck deck = new PlayerDeck(CARDS, List.of("strike", "defend"));
    List<String> snapshot = deck.getCardIds();

    assertThrows(UnsupportedOperationException.class, () -> snapshot.add("bandage"));

    assertIterableEquals(List.of("strike", "defend"), deck.getCardIds());
  }

  @Test
  void shouldCopyIndependently() {
    PlayerDeck original = new PlayerDeck(CARDS, List.of("strike", "defend"));
    PlayerDeck copy = original.copy();

    copy.addCard("bandage");
    original.removeCard("strike");

    assertIterableEquals(List.of("defend"), original.getCardIds());
    assertIterableEquals(List.of("strike", "defend", "bandage"), copy.getCardIds());
  }

  @Test
  void shouldClearDeck() {
    PlayerDeck deck = new PlayerDeck(CARDS, List.of("strike", "defend"));

    deck.clear();

    assertTrue(deck.isEmpty());
    assertEquals(0, deck.size());
  }
}

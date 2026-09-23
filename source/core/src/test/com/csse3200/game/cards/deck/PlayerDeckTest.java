package com.csse3200.game.cards.deck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
  private static final CardService UPGRADABLE_CARDS = TestCardService.withCardsUpgrade("strike");

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
    assertEquals(2, deck.countByCardId("strike"));
    assertEquals(1, deck.countByCardId("defend"));
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
    assertEquals(1, deck.countByCardId("strike"));
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

    assertThrows(IllegalArgumentException.class, () -> deck.addCard((String) null));
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
  void shouldGiveEachAddedCardAUniqueInstanceId() {
    PlayerDeck deck = new PlayerDeck(CARDS, List.of("strike", "strike"));

    List<CardInstance> cards = deck.getCards();

    assertEquals(2, cards.size());
    assertEquals("strike", cards.get(0).cardId());
    assertEquals("strike", cards.get(1).cardId());
    assertNotEquals(cards.get(0).instanceId(), cards.get(1).instanceId());
  }

  @Test
  void shouldClearDeck() {
    PlayerDeck deck = new PlayerDeck(CARDS, List.of("strike", "defend"));

    deck.clear();

    assertTrue(deck.isEmpty());
    assertEquals(0, deck.size());
  }

  @Test
  void shouldUpgradeOnlyTheTargetedCopy() {
    PlayerDeck deck =
        new PlayerDeck(UPGRADABLE_CARDS, List.of("strike", "strike", "strike", "strike"));
    List<CardInstance> before = deck.getCards();
    String id = before.get(2).instanceId();
    assertTrue(deck.upgradeCard(id));
    List<CardInstance> after = deck.getCards();
    assertFalse(before.get(2).isUpgraded());
    assertTrue(after.get(2).isUpgraded());
    assertFalse(after.get(0).isUpgraded());
    assertFalse(after.get(1).isUpgraded());
    assertFalse(after.get(3).isUpgraded());
    assertEquals(id, after.get(2).instanceId());
    assertEquals(4, after.size());
  }

  @Test
  void shouldReturnFalseWhenUpgradingTwice() {
    PlayerDeck deck = new PlayerDeck(UPGRADABLE_CARDS, List.of("strike"));
    String id = deck.getCards().getFirst().instanceId();
    assertTrue(deck.upgradeCard(id));
    assertFalse(deck.upgradeCard(id));
    assertTrue(deck.getCards().getFirst().isUpgraded());
  }

  @Test
  void shouldReturnFalseForUnknownInstanceId() {
    PlayerDeck deck = new PlayerDeck(UPGRADABLE_CARDS, List.of("strike", "strike"));
    assertFalse(deck.upgradeCard("no-such-instance"));
  }

  @Test
  void shouldReturnFalseWhenCardHasNoUpgradeDefinition() {
    PlayerDeck deck = new PlayerDeck(CARDS, List.of("bandage"));
    String id = deck.getCards().getFirst().instanceId();
    assertFalse(deck.upgradeCard(id));
  }

  @Test
  void shouldRejectInvalidInstanceIds() {
    PlayerDeck deck = new PlayerDeck(CARDS, List.of("bandage"));
    assertThrows(IllegalArgumentException.class, () -> deck.upgradeCard(null));
    assertThrows(IllegalArgumentException.class, () -> deck.upgradeCard(""));
  }
}

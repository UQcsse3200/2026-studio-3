package com.csse3200.game.cards.deck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class BattleDeckTest {
  @Test
  void shouldCreateDrawPileFromPlayerDeck() {
    PlayerDeck playerDeck = new PlayerDeck(List.of("strike", "defend", "bandage"));
    BattleDeck battleDeck = new BattleDeck(playerDeck);

    assertIterableEquals(List.of("strike", "defend", "bandage"), battleDeck.getDrawPile());
    assertTrue(battleDeck.getHand().isEmpty());
    assertTrue(battleDeck.getDiscardPile().isEmpty());
    assertEquals(3, battleDeck.getDrawPileSize());
    assertEquals(0, battleDeck.getHandSize());
    assertEquals(0, battleDeck.getDiscardPileSize());
  }

  @Test
  void shouldNotMutateOriginalPlayerDeck() {
    PlayerDeck playerDeck = new PlayerDeck(List.of("strike", "defend"));
    BattleDeck battleDeck = new BattleDeck(playerDeck);

    battleDeck.drawOne();

    assertIterableEquals(List.of("strike", "defend"), playerDeck.getCardIds());
    assertIterableEquals(List.of("defend"), battleDeck.getDrawPile());
  }

  @Test
  void shouldRejectNullPlayerDeck() {
    assertThrows(IllegalArgumentException.class, () -> new BattleDeck(null));
  }

  @Test
  void shouldDrawOneCardIntoHand() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike", "defend")));

    String drawnCard = battleDeck.drawOne();

    assertEquals("strike", drawnCard);
    assertIterableEquals(List.of("defend"), battleDeck.getDrawPile());
    assertIterableEquals(List.of("strike"), battleDeck.getHand());
    assertEquals(1, battleDeck.getDrawPileSize());
    assertEquals(1, battleDeck.getHandSize());
  }

  @Test
  void shouldReturnNullWhenDrawingFromEmptyDrawPile() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck());

    assertNull(battleDeck.drawOne());
    assertTrue(battleDeck.getDrawPile().isEmpty());
    assertTrue(battleDeck.getHand().isEmpty());
  }

  @Test
  void shouldDrawMultipleCardsIntoHand() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike", "defend", "bandage")));

    List<String> drawnCards = battleDeck.drawCards(2);

    assertIterableEquals(List.of("strike", "defend"), drawnCards);
    assertIterableEquals(List.of("bandage"), battleDeck.getDrawPile());
    assertIterableEquals(List.of("strike", "defend"), battleDeck.getHand());
  }

  @Test
  void shouldDrawOnlyAvailableCards() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike", "defend")));

    List<String> drawnCards = battleDeck.drawCards(5);

    assertIterableEquals(List.of("strike", "defend"), drawnCards);
    assertTrue(battleDeck.getDrawPile().isEmpty());
    assertIterableEquals(List.of("strike", "defend"), battleDeck.getHand());
  }

  @Test
  void shouldDrawNoCardsWhenCountIsZero() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike", "defend")));

    List<String> drawnCards = battleDeck.drawCards(0);

    assertTrue(drawnCards.isEmpty());
    assertIterableEquals(List.of("strike", "defend"), battleDeck.getDrawPile());
    assertTrue(battleDeck.getHand().isEmpty());
  }

  @Test
  void shouldRejectNegativeDrawCount() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike")));

    assertThrows(IllegalArgumentException.class, () -> battleDeck.drawCards(-1));
  }

  @Test
  void shouldReturnImmutableSnapshots() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike", "defend")));

    assertThrows(
        UnsupportedOperationException.class, () -> battleDeck.getDrawPile().add("bandage"));
    assertThrows(UnsupportedOperationException.class, () -> battleDeck.getHand().add("bandage"));
    assertThrows(
        UnsupportedOperationException.class, () -> battleDeck.getDiscardPile().add("bandage"));

    assertIterableEquals(List.of("strike", "defend"), battleDeck.getDrawPile());
    assertTrue(battleDeck.getHand().isEmpty());
    assertTrue(battleDeck.getDiscardPile().isEmpty());
  }

  @Test
  void shouldShuffleWithoutChangingCards() {
    List<String> startingCards =
        List.of("strike", "defend", "poison_dagger", "expose", "bandage", "inner_focus");
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(startingCards));

    battleDeck.shuffleDrawPile();

    List<String> expectedCards = new ArrayList<>(startingCards);
    List<String> actualCards = new ArrayList<>(battleDeck.getDrawPile());
    Collections.sort(expectedCards);
    Collections.sort(actualCards);
    assertIterableEquals(expectedCards, actualCards);
    assertEquals(startingCards.size(), battleDeck.getDrawPileSize());
    assertTrue(battleDeck.getHand().isEmpty());
  }

  @Test
  void shouldPlayCardFromHandIntoDiscardPile() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike", "defend")));
    battleDeck.drawOne();

    boolean played = battleDeck.playCard("strike");

    assertTrue(played);
    assertTrue(battleDeck.getHand().isEmpty());
    assertIterableEquals(List.of("strike"), battleDeck.getDiscardPile());
    assertIterableEquals(List.of("defend"), battleDeck.getDrawPile());
  }

  @Test
  void shouldNotPlayCardThatIsNotInHand() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike")));

    boolean played = battleDeck.playCard("strike");

    assertFalse(played);
    assertIterableEquals(List.of("strike"), battleDeck.getDrawPile());
    assertTrue(battleDeck.getHand().isEmpty());
    assertTrue(battleDeck.getDiscardPile().isEmpty());
  }

  @Test
  void shouldDiscardCardFromHand() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike", "defend", "bandage")));
    battleDeck.drawCards(2);

    boolean discarded = battleDeck.discardCard("defend");

    assertTrue(discarded);
    assertIterableEquals(List.of("strike"), battleDeck.getHand());
    assertIterableEquals(List.of("defend"), battleDeck.getDiscardPile());
    assertIterableEquals(List.of("bandage"), battleDeck.getDrawPile());
  }

  @Test
  void shouldNotDiscardNullCard() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike")));
    battleDeck.drawOne();

    boolean discarded = battleDeck.discardCard(null);

    assertFalse(discarded);
    assertIterableEquals(List.of("strike"), battleDeck.getHand());
    assertTrue(battleDeck.getDiscardPile().isEmpty());
  }

  @Test
  void shouldDiscardEntireHand() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike", "defend", "bandage")));
    battleDeck.drawCards(2);

    int discardedCount = battleDeck.discardHand();

    assertEquals(2, discardedCount);
    assertTrue(battleDeck.getHand().isEmpty());
    assertIterableEquals(List.of("strike", "defend"), battleDeck.getDiscardPile());
    assertIterableEquals(List.of("bandage"), battleDeck.getDrawPile());
  }

  @Test
  void shouldReturnZeroWhenDiscardingEmptyHand() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike")));

    int discardedCount = battleDeck.discardHand();

    assertEquals(0, discardedCount);
    assertIterableEquals(List.of("strike"), battleDeck.getDrawPile());
    assertTrue(battleDeck.getHand().isEmpty());
    assertTrue(battleDeck.getDiscardPile().isEmpty());
  }

  @Test
  void shouldReshuffleDiscardPileIntoEmptyDrawPile() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike")));
    battleDeck.drawOne();
    battleDeck.discardCard("strike");

    boolean reshuffled = battleDeck.reshuffleDiscardIntoDrawPile();

    assertTrue(reshuffled);
    assertIterableEquals(List.of("strike"), battleDeck.getDrawPile());
    assertTrue(battleDeck.getHand().isEmpty());
    assertTrue(battleDeck.getDiscardPile().isEmpty());
  }

  @Test
  void shouldNotReshuffleWhenDrawPileIsNotEmpty() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike", "defend")));
    battleDeck.drawOne();
    battleDeck.discardCard("strike");

    boolean reshuffled = battleDeck.reshuffleDiscardIntoDrawPile();

    assertFalse(reshuffled);
    assertIterableEquals(List.of("defend"), battleDeck.getDrawPile());
    assertIterableEquals(List.of("strike"), battleDeck.getDiscardPile());
  }

  @Test
  void shouldDrawFromReshuffledDiscardPile() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike")));
    assertEquals("strike", battleDeck.drawOne());
    assertTrue(battleDeck.discardCard("strike"));

    String redrawnCard = battleDeck.drawOne();

    assertEquals("strike", redrawnCard);
    assertIterableEquals(List.of("strike"), battleDeck.getHand());
    assertTrue(battleDeck.getDrawPile().isEmpty());
    assertTrue(battleDeck.getDiscardPile().isEmpty());
  }
}

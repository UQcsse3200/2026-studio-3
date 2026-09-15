package com.csse3200.game.cards.deck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.TestCardService;
import com.csse3200.game.cards.runtime.CardInstance;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class BattleDeckTest {
  private static final CardService CARDS =
      TestCardService.withCards(
          "strike", "defend", "poison_dagger", "expose", "inner_focus", "bandage");

  @Test
  void shouldCreateDrawPileFromPlayerDeck() {
    PlayerDeck playerDeck = new PlayerDeck(CARDS, List.of("strike", "defend", "bandage"));
    BattleDeck battleDeck = new BattleDeck(playerDeck);

    assertIterableEquals(
        List.of("strike", "defend", "bandage"),
        battleDeck.getDrawPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
    assertTrue(
        battleDeck.getHand().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList()
            .isEmpty());
    assertTrue(
        battleDeck.getDiscardPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList()
            .isEmpty());
    assertEquals(3, battleDeck.getDrawPileSize());
    assertEquals(0, battleDeck.getHandSize());
    assertEquals(0, battleDeck.getDiscardPileSize());
  }

  @Test
  void shouldNotMutateOriginalPlayerDeck() {
    PlayerDeck playerDeck = new PlayerDeck(CARDS, List.of("strike", "defend"));
    BattleDeck battleDeck = new BattleDeck(playerDeck);

    battleDeck.drawOne();

    assertIterableEquals(List.of("strike", "defend"), playerDeck.getCardIds());
    assertIterableEquals(
        List.of("defend"),
        battleDeck.getDrawPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
  }

  @Test
  void shouldRejectNullPlayerDeck() {
    assertThrows(IllegalArgumentException.class, () -> new BattleDeck(null));
  }

  @Test
  void shouldDrawOneCardIntoHand() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(CARDS, List.of("strike", "defend")));

    CardInstance drawnCard = battleDeck.drawOne();

    assertEquals("strike", drawnCard.cardId());
    assertIterableEquals(
        List.of("defend"),
        battleDeck.getDrawPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
    assertIterableEquals(
        List.of("strike"),
        battleDeck.getHand().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
    assertEquals(1, battleDeck.getDrawPileSize());
    assertEquals(1, battleDeck.getHandSize());
  }

  @Test
  void shouldReturnNullWhenDrawingFromEmptyDrawPile() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(CARDS));

    assertNull(battleDeck.drawOne());
    assertTrue(
        battleDeck.getDrawPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList()
            .isEmpty());
    assertTrue(
        battleDeck.getHand().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList()
            .isEmpty());
  }

  @Test
  void shouldDrawMultipleCardsIntoHand() {
    BattleDeck battleDeck =
        new BattleDeck(new PlayerDeck(CARDS, List.of("strike", "defend", "bandage")));

    List<CardInstance> drawnCards = battleDeck.drawCards(2);

    assertIterableEquals(
        List.of("strike", "defend"), drawnCards.stream().map(CardInstance::cardId).toList());
    assertIterableEquals(
        List.of("bandage"),
        battleDeck.getDrawPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
    assertIterableEquals(
        List.of("strike", "defend"),
        battleDeck.getHand().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
  }

  @Test
  void shouldDrawOnlyAvailableCards() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(CARDS, List.of("strike", "defend")));

    List<CardInstance> drawnCards = battleDeck.drawCards(5);

    assertIterableEquals(
        List.of("strike", "defend"), drawnCards.stream().map(CardInstance::cardId).toList());
    assertTrue(
        battleDeck.getDrawPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList()
            .isEmpty());
    assertIterableEquals(
        List.of("strike", "defend"),
        battleDeck.getHand().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
  }

  @Test
  void shouldDrawNoCardsWhenCountIsZero() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(CARDS, List.of("strike", "defend")));

    List<CardInstance> drawnCards = battleDeck.drawCards(0);

    assertTrue(drawnCards.isEmpty());
    assertIterableEquals(
        List.of("strike", "defend"),
        battleDeck.getDrawPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
    assertTrue(
        battleDeck.getHand().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList()
            .isEmpty());
  }

  @Test
  void shouldRejectNegativeDrawCount() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(CARDS, List.of("strike")));

    assertThrows(IllegalArgumentException.class, () -> battleDeck.drawCards(-1));
  }

  @Test
  void shouldReturnImmutableSnapshots() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(CARDS, List.of("strike", "defend")));

    assertThrows(
        UnsupportedOperationException.class,
        () ->
            battleDeck.getDrawPile().stream()
                .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
                .toList()
                .add("bandage"));
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            battleDeck.getHand().stream()
                .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
                .toList()
                .add("bandage"));
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            battleDeck.getDiscardPile().stream()
                .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
                .toList()
                .add("bandage"));

    assertIterableEquals(
        List.of("strike", "defend"),
        battleDeck.getDrawPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
    assertTrue(
        battleDeck.getHand().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList()
            .isEmpty());
    assertTrue(
        battleDeck.getDiscardPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList()
            .isEmpty());
  }

  @Test
  void shouldShuffleWithoutChangingCards() {
    List<String> startingCards =
        List.of("strike", "defend", "poison_dagger", "expose", "bandage", "inner_focus");
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(CARDS, startingCards));

    battleDeck.shuffleDrawPile();

    List<String> expectedCards = new ArrayList<>(startingCards);
    List<String> actualCards =
        new ArrayList<>(
            battleDeck.getDrawPile().stream()
                .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
                .toList());
    Collections.sort(expectedCards);
    Collections.sort(actualCards);
    assertIterableEquals(expectedCards, actualCards);
    assertEquals(startingCards.size(), battleDeck.getDrawPileSize());
    assertTrue(
        battleDeck.getHand().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList()
            .isEmpty());
  }

  @Test
  void shouldPlayCardFromHandIntoDiscardPile() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(CARDS, List.of("strike", "defend")));
    battleDeck.drawOne();

    boolean played =
        battleDeck.playCard(
            battleDeck.getHand().isEmpty() ? "missing" : battleDeck.getHand().get(0).instanceId());

    assertTrue(played);
    assertTrue(
        battleDeck.getHand().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList()
            .isEmpty());
    assertIterableEquals(
        List.of("strike"),
        battleDeck.getDiscardPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
    assertIterableEquals(
        List.of("defend"),
        battleDeck.getDrawPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
  }

  @Test
  void shouldNotPlayCardThatIsNotInHand() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(CARDS, List.of("strike")));

    boolean played =
        battleDeck.playCard(
            battleDeck.getHand().isEmpty() ? "missing" : battleDeck.getHand().get(0).instanceId());

    assertFalse(played);
    assertIterableEquals(
        List.of("strike"),
        battleDeck.getDrawPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
    assertTrue(
        battleDeck.getHand().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList()
            .isEmpty());
    assertTrue(
        battleDeck.getDiscardPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList()
            .isEmpty());
  }

  @Test
  void shouldDiscardCardFromHand() {
    BattleDeck battleDeck =
        new BattleDeck(new PlayerDeck(CARDS, List.of("strike", "defend", "bandage")));
    battleDeck.drawCards(2);

    boolean discarded = battleDeck.discardCard(battleDeck.getHand().get(1).instanceId());

    assertTrue(discarded);
    assertIterableEquals(
        List.of("strike"),
        battleDeck.getHand().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
    assertIterableEquals(
        List.of("defend"),
        battleDeck.getDiscardPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
    assertIterableEquals(
        List.of("bandage"),
        battleDeck.getDrawPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
  }

  @Test
  void shouldNotDiscardNullCard() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(CARDS, List.of("strike")));
    battleDeck.drawOne();

    boolean discarded = battleDeck.discardCard(null);

    assertFalse(discarded);
    assertIterableEquals(
        List.of("strike"),
        battleDeck.getHand().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
    assertTrue(
        battleDeck.getDiscardPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList()
            .isEmpty());
  }

  @Test
  void shouldDiscardEntireHand() {
    BattleDeck battleDeck =
        new BattleDeck(new PlayerDeck(CARDS, List.of("strike", "defend", "bandage")));
    battleDeck.drawCards(2);

    int discardedCount = battleDeck.discardHand();

    assertEquals(2, discardedCount);
    assertTrue(
        battleDeck.getHand().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList()
            .isEmpty());
    assertIterableEquals(
        List.of("strike", "defend"),
        battleDeck.getDiscardPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
    assertIterableEquals(
        List.of("bandage"),
        battleDeck.getDrawPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
  }

  @Test
  void shouldReturnZeroWhenDiscardingEmptyHand() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(CARDS, List.of("strike")));

    int discardedCount = battleDeck.discardHand();

    assertEquals(0, discardedCount);
    assertIterableEquals(
        List.of("strike"),
        battleDeck.getDrawPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
    assertTrue(
        battleDeck.getHand().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList()
            .isEmpty());
    assertTrue(
        battleDeck.getDiscardPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList()
            .isEmpty());
  }

  @Test
  void shouldReshuffleDiscardPileIntoEmptyDrawPile() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(CARDS, List.of("strike")));
    battleDeck.drawOne();
    battleDeck.discardCard(battleDeck.getHand().get(0).instanceId());

    boolean reshuffled = battleDeck.reshuffleDiscardIntoDrawPile();

    assertTrue(reshuffled);
    assertIterableEquals(
        List.of("strike"),
        battleDeck.getDrawPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
    assertTrue(
        battleDeck.getHand().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList()
            .isEmpty());
    assertTrue(
        battleDeck.getDiscardPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList()
            .isEmpty());
  }

  @Test
  void shouldNotReshuffleWhenDrawPileIsNotEmpty() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(CARDS, List.of("strike", "defend")));
    battleDeck.drawOne();
    battleDeck.discardCard(battleDeck.getHand().get(0).instanceId());

    boolean reshuffled = battleDeck.reshuffleDiscardIntoDrawPile();

    assertFalse(reshuffled);
    assertIterableEquals(
        List.of("defend"),
        battleDeck.getDrawPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
    assertIterableEquals(
        List.of("strike"),
        battleDeck.getDiscardPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
  }

  @Test
  void shouldDrawFromReshuffledDiscardPile() {
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(CARDS, List.of("strike")));
    assertEquals("strike", battleDeck.drawOne().cardId());
    assertTrue(battleDeck.discardCard(battleDeck.getHand().get(0).instanceId()));

    CardInstance redrawnCard = battleDeck.drawOne();

    assertEquals("strike", redrawnCard.cardId());
    assertIterableEquals(
        List.of("strike"),
        battleDeck.getHand().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList());
    assertTrue(
        battleDeck.getDrawPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList()
            .isEmpty());
    assertTrue(
        battleDeck.getDiscardPile().stream()
            .map(com.csse3200.game.cards.runtime.CardInstance::cardId)
            .toList()
            .isEmpty());
  }
}

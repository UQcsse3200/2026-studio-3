package com.csse3200.game.cards.fusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.RunState;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CardFusionServiceTest {
  private CardService cardService;
  private CardFusionService fusionService;
  private RunState runState;
  private PlayerDeck deck;

  @BeforeEach
  void setUp() {
    cardService = new CardLibrary(CardConfigLoader.loadCards());
    fusionService = new CardFusionService(cardService, new Random(42));
    runState = new RunState();
    deck = runState.getOrCreatePlayerDeck(cardService);
    deck.clear();
  }

  @Test
  void shouldConsumeThreeCommonCardsAndAddOneRareReward() {
    deck.addCards(List.of("strike", "defend", "bandage", "poison_dagger"));
    List<String> selectedInstanceIds = firstThreeInstanceIds(deck);

    CardFusionResult result = fusionService.fuse(runState, selectedInstanceIds);

    assertTrue(result.successful());
    String rewardId = result.rewardedCardId().orElseThrow();
    assertEquals(CardFusionFailureReason.NONE, result.failureReason());
    assertEquals(2, deck.size());
    assertTrue(deck.contains("poison_dagger"));
    assertTrue(deck.contains(rewardId));
    assertEquals(Rarity.RARE, cardService.getCard(rewardId).orElseThrow().rarity);
    assertTrue(runState.hasUsedCardFusion());
  }

  @Test
  void shouldAllowThreeCopiesOfTheSameCommonCard() {
    deck.addCards(List.of("strike", "strike", "strike"));

    CardFusionResult result = fusionService.fuse(runState, firstThreeInstanceIds(deck));

    assertTrue(result.successful());
    assertEquals(1, deck.size());
    assertEquals(Rarity.RARE, cardService.getCard(deck.getCardIds().get(0)).orElseThrow().rarity);
  }

  @Test
  void shouldRejectSelectionsThatContainANonCommonCardWithoutMutatingTheDeck() {
    deck.addCards(List.of("strike", "defend", "poison_dagger"));
    List<String> before = deck.getCardIds();

    CardFusionResult result = fusionService.fuse(runState, firstThreeInstanceIds(deck));

    assertFalse(result.successful());
    assertEquals(CardFusionFailureReason.CARD_NOT_COMMON, result.failureReason());
    assertIterableEquals(before, deck.getCardIds());
    assertFalse(runState.hasUsedCardFusion());
  }

  @Test
  void shouldRejectDuplicateInstanceSelectionsWithoutMutatingTheDeck() {
    deck.addCards(List.of("strike", "strike", "strike"));
    String firstInstanceId = deck.getCards().get(0).instanceId();
    List<String> before = deck.getCardIds();

    CardFusionResult result =
        fusionService.fuse(runState, List.of(firstInstanceId, firstInstanceId, firstInstanceId));

    assertFalse(result.successful());
    assertEquals(CardFusionFailureReason.INVALID_SELECTION, result.failureReason());
    assertIterableEquals(before, deck.getCardIds());
    assertFalse(runState.hasUsedCardFusion());
  }

  @Test
  void shouldOnlyAllowOneFusionPerRun() {
    deck.addCards(List.of("strike", "defend", "bandage", "strike", "defend", "bandage"));
    CardFusionResult first = fusionService.fuse(runState, firstThreeInstanceIds(deck));
    List<String> afterFirstFusion = deck.getCardIds();

    CardFusionResult second = fusionService.fuse(runState, firstThreeInstanceIds(deck));

    assertTrue(first.successful());
    assertFalse(second.successful());
    assertEquals(CardFusionFailureReason.FUSION_ALREADY_USED, second.failureReason());
    assertIterableEquals(afterFirstFusion, deck.getCardIds());
  }

  @Test
  void shouldOnlyExposeCommonCardsAsEligibleForFusion() {
    deck.addCards(List.of("strike", "poison_dagger", "inner_focus", "defend"));

    List<String> eligibleCardIds =
        fusionService.getEligibleCards(runState).stream().map(CardInstance::cardId).toList();

    assertIterableEquals(List.of("strike", "defend"), eligibleCardIds);
  }

  private static List<String> firstThreeInstanceIds(PlayerDeck deck) {
    return deck.getCards().stream().limit(3).map(CardInstance::instanceId).toList();
  }
}

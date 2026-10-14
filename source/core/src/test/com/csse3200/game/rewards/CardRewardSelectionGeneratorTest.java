package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardAcquisitionPool;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.extensions.GameExtension;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CardRewardSelectionGeneratorTest {
  private CardService cards;

  @BeforeEach
  void setUp() {
    cards = new CardLibrary(CardConfigLoader.loadCards());
  }

  @Test
  void shouldGenerateThreeDistinctEligibleCardsDeterministically() {
    CardAcquisitionPool pool =
        new CardAcquisitionPool(cards, List.of("strike", "defend", "bandage", "expose"));
    CardRewardSelection first =
        new CardRewardSelectionGenerator(pool, new Random(42)).generate().orElseThrow();
    CardRewardSelection second =
        new CardRewardSelectionGenerator(pool, new Random(42)).generate().orElseThrow();

    assertEquals(first, second);
    assertEquals(3, first.cardIds().size());
    assertEquals(3, new HashSet<>(first.cardIds()).size());
    assertTrue(first.cardIds().stream().allMatch(pool::isEligible));
    assertEquals(4, pool.eligibleCardIds().size());
  }

  @Test
  void shouldDegradeToAvailableCandidatesAndHandleEmptyPool() {
    CardRewardSelection one =
        new CardRewardSelectionGenerator(
                new CardAcquisitionPool(cards, List.of("strike")), new Random(1))
            .generate()
            .orElseThrow();
    CardRewardSelection two =
        new CardRewardSelectionGenerator(
                new CardAcquisitionPool(cards, List.of("strike", "defend")), new Random(1))
            .generate()
            .orElseThrow();

    assertEquals(1, one.cardIds().size());
    assertEquals(2, two.cardIds().size());
    assertFalse(
        new CardRewardSelectionGenerator(new CardAcquisitionPool(cards, List.of()), new Random(1))
            .generate()
            .isPresent());
  }

  @Test
  void shouldRejectInvalidRequestedCounts() {
    CardRewardSelectionGenerator generator =
        new CardRewardSelectionGenerator(
            new CardAcquisitionPool(cards, List.of("strike")), new Random(1));

    assertThrows(IllegalArgumentException.class, () -> generator.generate(0));
    assertThrows(IllegalArgumentException.class, () -> generator.generate(4));
  }

  @Test
  void differentRepresentativeSeedsShouldProduceDifferentOrderedOffers() {
    CardAcquisitionPool pool =
        new CardAcquisitionPool(cards, cards.getAllCards().stream().map(card -> card.id).toList());

    CardRewardSelection first =
        new CardRewardSelectionGenerator(pool, new Random(1)).generate().orElseThrow();
    CardRewardSelection second =
        new CardRewardSelectionGenerator(pool, new Random(2)).generate().orElseThrow();

    assertNotEquals(first.cardIds(), second.cardIds());
  }
}

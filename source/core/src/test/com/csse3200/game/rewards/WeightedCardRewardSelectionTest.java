package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.cards.CardAcquisitionPool;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.extensions.GameExtension;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class WeightedCardRewardSelectionTest {
  private CardLibrary cards;

  @BeforeEach
  void setUp() {
    cards = new CardLibrary(CardConfigLoader.loadCards());
  }

  @Test
  void exactBoundariesChooseTheFollowingRarityAndNeverPickZeroWeightBuckets() {
    CardAcquisitionPool pool =
        new CardAcquisitionPool(cards, List.of("strike", "starfall", "resurrection"));
    CardRewardPolicy policy = policy(2, 3, 5);
    assertEquals("strike", first(pool, policy, 0));
    assertEquals("strike", first(pool, policy, Math.nextDown(0.2)));
    assertEquals("starfall", first(pool, policy, 0.2));
    assertEquals("starfall", first(pool, policy, Math.nextDown(0.5)));
    assertEquals("resurrection", first(pool, policy, 0.5));
    assertEquals("resurrection", first(pool, policy, Math.nextDown(1d)));
    assertEquals("starfall", first(pool, policy(0, 1, 0), 0));
  }

  @Test
  void selectsUniformlyWithinStableRarityBucket() {
    CardAcquisitionPool pool = new CardAcquisitionPool(cards, List.of("strike", "defend"));
    Random random =
        new Random() {
          @Override
          public double nextDouble() {
            return 0d;
          }

          @Override
          public int nextInt(int bound) {
            assertEquals(2, bound);
            return 1;
          }
        };
    assertEquals(
        List.of("strike"),
        new CardRewardSelectionGenerator(pool, policy(1, 0, 0), CardRewardProfile.NORMAL, random)
            .generate(1)
            .orElseThrow()
            .cardIds());
  }

  @Test
  void renormalizesAfterExhaustionAndKeepsExclusionsWhenFewerThanThreeCanBeChosen() {
    CardAcquisitionPool pool =
        new CardAcquisitionPool(cards, List.of("strike", "defend", "starfall", "resurrection"));
    CardRewardSelectionGenerator generator =
        new CardRewardSelectionGenerator(
            pool, policy(1, 1, 0), CardRewardProfile.NORMAL, constant(0.99));
    assertEquals(
        List.of("starfall", "defend", "strike"), generator.generate().orElseThrow().cardIds());
    assertEquals(4, pool.eligibleCardIds().size());
    assertEquals(
        List.of("starfall"),
        new CardRewardSelectionGenerator(
                pool, policy(0, 1, 0), CardRewardProfile.NORMAL, new Random(3))
            .generate()
            .orElseThrow()
            .cardIds());
    CardAcquisitionPool noRare = new CardAcquisitionPool(cards, List.of("strike", "starfall"));
    assertTrue(
        new CardRewardSelectionGenerator(
                noRare, policy(0, 0, 1), CardRewardProfile.NORMAL, new Random())
            .generate()
            .isEmpty());
  }

  @Test
  void handlesEmptyPoolsMissingRaritiesAndLargeFiniteWeights() {
    CardRewardPolicy large = policy(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
    CardAcquisitionPool pool =
        new CardAcquisitionPool(cards, List.of("strike", "starfall", "resurrection"));
    assertEquals("starfall", first(pool, large, 0.5));
    assertTrue(
        new CardRewardSelectionGenerator(
                new CardAcquisitionPool(cards, List.of()),
                large,
                CardRewardProfile.NORMAL,
                new Random())
            .generate()
            .isEmpty());
    assertEquals(
        List.of("strike"),
        new CardRewardSelectionGenerator(
                new CardAcquisitionPool(cards, List.of("strike")),
                large,
                CardRewardProfile.NORMAL,
                new Random())
            .generate()
            .orElseThrow()
            .cardIds());
  }

  @Test
  void profilesAreIndependentAndSeededOffersAreDistinctImmutableAndReproducible() {
    CardRewardPolicy policy =
        new CardRewardPolicy(
            new CardRewardPolicy.RarityWeights(1, 0, 0),
            new CardRewardPolicy.RarityWeights(0, 0, 1));
    CardAcquisitionPool pool =
        new CardAcquisitionPool(cards, cards.getAllCards().stream().map(card -> card.id).toList());
    CardRewardSelectionGenerator normal =
        new CardRewardSelectionGenerator(pool, policy, CardRewardProfile.NORMAL, new Random(42));
    CardRewardSelectionGenerator repeated =
        new CardRewardSelectionGenerator(pool, policy, CardRewardProfile.NORMAL, new Random(42));
    CardRewardSelectionGenerator elite =
        new CardRewardSelectionGenerator(pool, policy, CardRewardProfile.ELITE, new Random(42));
    for (int i = 0; i < 25; i++) {
      CardRewardSelection offer = normal.generate().orElseThrow();
      assertEquals(offer, repeated.generate().orElseThrow());
      assertEquals(3, new HashSet<>(offer.cardIds()).size());
      assertTrue(
          offer.cardIds().stream()
              .allMatch(
                  id ->
                      cards.getCard(id).orElseThrow().rarity
                          == com.csse3200.game.cards.Rarity.COMMON));
      assertTrue(
          elite.generate().orElseThrow().cardIds().stream()
              .allMatch(
                  id ->
                      cards.getCard(id).orElseThrow().rarity
                          == com.csse3200.game.cards.Rarity.RARE));
      assertThrows(UnsupportedOperationException.class, () -> offer.cardIds().add("strike"));
    }
  }

  private static CardRewardPolicy policy(double common, double uncommon, double rare) {
    var weights = new CardRewardPolicy.RarityWeights(common, uncommon, rare);
    return new CardRewardPolicy(weights, weights);
  }

  private static String first(CardAcquisitionPool pool, CardRewardPolicy policy, double draw) {
    return new CardRewardSelectionGenerator(pool, policy, CardRewardProfile.NORMAL, constant(draw))
        .generate(1)
        .orElseThrow()
        .cardIds()
        .get(0);
  }

  private static Random constant(double draw) {
    return new Random() {
      @Override
      public double nextDouble() {
        return draw;
      }

      @Override
      public int nextInt(int bound) {
        return 0;
      }
    };
  }
}

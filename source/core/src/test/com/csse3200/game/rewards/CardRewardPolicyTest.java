package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CardRewardPolicyTest {
  private static final String VALID =
      "{\"normal\":{\"COMMON\":7,\"UNCOMMON\":2,\"RARE\":1},\"elite\":{\"COMMON\":1,\"UNCOMMON\":2,\"RARE\":7}}";

  @Test
  void parsesRelativeWeightsAndKeepsProfilesIndependent() {
    CardRewardPolicy policy = CardRewardPolicyLoader.parse(VALID);
    assertEquals(
        new CardRewardPolicy.RarityWeights(7, 2, 1), policy.weightsFor(CardRewardProfile.NORMAL));
    assertEquals(
        new CardRewardPolicy.RarityWeights(1, 2, 7), policy.weightsFor(CardRewardProfile.ELITE));
  }

  @Test
  void rejectsMissingUnknownDuplicateAndIncorrectlyTypedFields() {
    for (String json :
        new String[] {
          "{}",
          "[]",
          "null",
          "{broken",
          VALID.replace("\"elite\"", "\"boss\""),
          VALID.replace("\"COMMON\":7,", ""),
          VALID.replace("\"COMMON\":7", "\"MYTHIC\":7"),
          VALID.replace("\"COMMON\":7", "\"COMMON\":7,\"COMMON\":8"),
          VALID.replace("\"COMMON\":7", "\"COMMON\":\"7\""),
          VALID.replace("\"COMMON\":7", "\"COMMON\":null"),
          VALID.replace("\"COMMON\":7", "\"COMMON\":true")
        }) {
      assertThrows(IllegalArgumentException.class, () -> CardRewardPolicyLoader.parse(json), json);
    }
  }

  @Test
  void rejectsNegativeNonfiniteAndAllZeroWeightsWithUsefulProfileErrors() {
    for (double weight :
        new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
      assertThrows(
          IllegalArgumentException.class, () -> new CardRewardPolicy.RarityWeights(weight, 1, 1));
    }
    assertThrows(IllegalArgumentException.class, () -> new CardRewardPolicy.RarityWeights(0, 0, 0));
    assertTrue(
        assertThrows(
                IllegalArgumentException.class,
                () -> CardRewardPolicyLoader.parse(VALID.replace("\"COMMON\":7", "\"COMMON\":-1")))
            .getMessage()
            .contains("normal"));
    assertTrue(
        assertThrows(
                IllegalArgumentException.class,
                () -> CardRewardPolicyLoader.parse(VALID.replace("\"RARE\":7", "\"RARE\":1e999")))
            .getMessage()
            .contains("elite"));
  }

  @Test
  void permitsZeroExclusionsAndVeryLargeFiniteRelativeWeights() {
    assertEquals(0d, new CardRewardPolicy.RarityWeights(0, 0, 5).common());
    assertDoesNotThrow(
        () ->
            new CardRewardPolicy.RarityWeights(
                Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE));
  }
}

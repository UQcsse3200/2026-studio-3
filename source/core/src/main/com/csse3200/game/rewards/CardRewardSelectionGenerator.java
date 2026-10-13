package com.csse3200.game.rewards;

import com.csse3200.game.cards.CardAcquisitionPool;
import com.csse3200.game.cards.Rarity;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Samples a rarity-weighted, distinct Card reward without consuming the shared acquisition pool.
 */
public final class CardRewardSelectionGenerator {
  private final CardAcquisitionPool pool;
  private final Random random;
  private final CardRewardPolicy.RarityWeights weights;

  public CardRewardSelectionGenerator(CardAcquisitionPool pool) {
    this(pool, new Random());
  }

  public CardRewardSelectionGenerator(CardAcquisitionPool pool, Random random) {
    this(pool, CardRewardPolicyLoader.loadDefault(), CardRewardProfile.NORMAL, random);
  }

  /** Captures validated weights once; generating or displaying an offer does not reload JSON. */
  public CardRewardSelectionGenerator(
      CardAcquisitionPool pool, CardRewardPolicy policy, CardRewardProfile profile, Random random) {
    this.pool = Objects.requireNonNull(pool, "pool cannot be null");
    this.random = Objects.requireNonNull(random, "random cannot be null");
    this.weights = Objects.requireNonNull(policy, "policy cannot be null").weightsFor(profile);
  }

  /** Generates up to three choices, or empty when no eligible rarity has positive weight. */
  public Optional<CardRewardSelection> generate() {
    return generate(CardRewardSelection.MAX_CHOICES);
  }

  /** Generates up to {@code count} distinct choices without mutating the shared pool. */
  public Optional<CardRewardSelection> generate(int count) {
    if (count <= 0 || count > CardRewardSelection.MAX_CHOICES) {
      throw new IllegalArgumentException("count must be between 1 and 3");
    }
    EnumMap<Rarity, List<String>> buckets = new EnumMap<>(Rarity.class);
    for (Rarity rarity : Rarity.values()) {
      buckets.put(rarity, new ArrayList<>());
    }
    pool.getEligibleCards().forEach(card -> buckets.get(card.rarity).add(card.id));
    List<String> selected = new ArrayList<>();
    while (selected.size() < count) {
      // Scaling by the largest available weight avoids overflow, including finite values whose
      // raw sum exceeds Double.MAX_VALUE. Empty and zero-weight buckets cannot be selected.
      double maximum = 0d;
      for (Rarity rarity : Rarity.values()) {
        if (!buckets.get(rarity).isEmpty()) {
          maximum = Math.max(maximum, weights.weight(rarity));
        }
      }
      if (maximum == 0d) {
        break;
      }
      double total = 0d;
      Rarity lastAvailable = null;
      for (Rarity rarity : Rarity.values()) {
        if (!buckets.get(rarity).isEmpty() && weights.weight(rarity) > 0d) {
          total += weights.weight(rarity) / maximum;
          lastAvailable = rarity;
        }
      }
      double draw = random.nextDouble() * total;
      Rarity chosen = lastAvailable;
      for (Rarity rarity : Rarity.values()) {
        if (buckets.get(rarity).isEmpty() || weights.weight(rarity) == 0d) {
          continue;
        }
        draw -= weights.weight(rarity) / maximum;
        if (draw < 0d) {
          chosen = rarity;
          break;
        }
      }
      List<String> bucket = buckets.get(chosen);
      selected.add(bucket.remove(random.nextInt(bucket.size())));
    }
    return selected.isEmpty() ? Optional.empty() : Optional.of(new CardRewardSelection(selected));
  }
}

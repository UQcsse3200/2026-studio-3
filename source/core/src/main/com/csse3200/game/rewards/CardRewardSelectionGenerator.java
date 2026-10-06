package com.csse3200.game.rewards;

import com.csse3200.game.cards.CardAcquisitionPool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/** Samples one fixed, non-repeating Card reward from the shared acquisition pool. */
public final class CardRewardSelectionGenerator {
  private final CardAcquisitionPool pool;
  private final Random random;

  public CardRewardSelectionGenerator(CardAcquisitionPool pool) {
    this(pool, new Random());
  }

  public CardRewardSelectionGenerator(CardAcquisitionPool pool, Random random) {
    this.pool = Objects.requireNonNull(pool, "pool cannot be null");
    this.random = Objects.requireNonNull(random, "random cannot be null");
  }

  /** Generates up to three choices, or empty when no definition is eligible. */
  public Optional<CardRewardSelection> generate() {
    return generate(CardRewardSelection.MAX_CHOICES);
  }

  /** Generates up to {@code count} distinct choices without mutating the shared pool. */
  public Optional<CardRewardSelection> generate(int count) {
    if (count <= 0 || count > CardRewardSelection.MAX_CHOICES) {
      throw new IllegalArgumentException("count must be between 1 and 3");
    }
    List<String> candidates = new ArrayList<>(pool.eligibleCardIds());
    if (candidates.isEmpty()) {
      return Optional.empty();
    }
    Collections.shuffle(candidates, random);
    int selectedCount = Math.min(count, candidates.size());
    return Optional.of(new CardRewardSelection(candidates.subList(0, selectedCount)));
  }
}

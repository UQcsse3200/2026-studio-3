package com.csse3200.game.rewards;

import com.csse3200.game.cards.Rarity;
import java.util.Objects;

/** Validated immutable relative rarity weights for normal and elite card rewards. */
public record CardRewardPolicy(RarityWeights normal, RarityWeights elite) {
  public CardRewardPolicy {
    Objects.requireNonNull(normal, "normal reward profile is required");
    Objects.requireNonNull(elite, "elite reward profile is required");
  }

  public RarityWeights weightsFor(CardRewardProfile profile) {
    return switch (Objects.requireNonNull(profile, "reward profile is required")) {
      case NORMAL -> normal;
      case ELITE -> elite;
    };
  }

  /** Weights need not sum to 1 or 100. Zero excludes a rarity from selection. */
  public record RarityWeights(double common, double uncommon, double rare) {
    public RarityWeights {
      validate("COMMON", common);
      validate("UNCOMMON", uncommon);
      validate("RARE", rare);
      if (Math.max(common, Math.max(uncommon, rare)) == 0d) {
        throw new IllegalArgumentException("At least one rarity weight must be positive");
      }
    }

    public double weight(Rarity rarity) {
      return switch (rarity) {
        case COMMON -> common;
        case UNCOMMON -> uncommon;
        case RARE -> rare;
      };
    }

    private static void validate(String rarity, double value) {
      if (!Double.isFinite(value) || value < 0d) {
        throw new IllegalArgumentException(rarity + " weight must be finite and nonnegative");
      }
    }
  }
}

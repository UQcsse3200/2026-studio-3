package com.csse3200.game.cards.runtime;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.CardUpgradeConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import java.util.Arrays;
import java.util.List;

/** Resolves one card instance to the exact values used by both UI and gameplay. */
public final class CardResolver {
  /**
   * Selects base or upgraded values without modifying either input.
   *
   * @param config shared card definition
   * @param instance exact runtime card copy
   * @return an immutable, copy-safe resolved card
   */
  public ResolvedCard resolve(CardConfig config, CardInstance instance) {
    if (config == null) {
      throw new IllegalArgumentException("config must not be null");
    }
    if (instance == null) {
      throw new IllegalArgumentException("instance must not be null");
    }
    if (!instance.cardId().equals(config.id)) {
      throw new IllegalArgumentException(
          "Card instance ID '"
              + instance.cardId()
              + "' does not match config ID '"
              + config.id
              + "'");
    }

    if (!instance.isUpgraded()) {
      return resolvedCard(
          config,
          instance,
          config.name,
          config.description,
          config.cost,
          config.rarity,
          config.effects,
          false);
    }

    CardUpgradeConfig upgrade = config.upgrade;
    if (upgrade == null) {
      throw new IllegalStateException(
          "Card instance is upgraded but its config has no upgrade definition: " + config.id);
    }
    return resolvedCard(
        config,
        instance,
        upgrade.name,
        upgrade.description,
        upgrade.cost,
        upgrade.rarity,
        upgrade.effects,
        true);
  }

  private static ResolvedCard resolvedCard(
      CardConfig config,
      CardInstance instance,
      String name,
      String description,
      int cost,
      com.csse3200.game.cards.Rarity rarity,
      EffectConfig[] effects,
      boolean upgraded) {
    List<EffectConfig> effectList = effects == null ? null : Arrays.asList(effects);
    return new ResolvedCard(
        instance.instanceId(),
        config.id,
        name,
        description,
        cost,
        config.type,
        rarity,
        config.target,
        effectList,
        config.texturePath,
        upgraded);
  }
}

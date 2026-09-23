package com.csse3200.game.cards.acquisition;

import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.CardUpgradeConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Shared allow-list for shop and event card acquisition.
 *
 * <p>Excludes cards that still rely on effects dropped by the live {@code CardEffectHandler}
 * (currently {@link EffectType#SUNDER} and {@link EffectType#PIERCE}). Member 5 acquisition and
 * Member 2 post-battle rewards should share this list so players only receive cards that resolve in
 * real combat.
 */
public final class CardAcquisitionAllowList {
  private static final Set<EffectType> EXCLUDED_LIVE_EFFECTS =
      EnumSet.of(EffectType.SUNDER, EffectType.PIERCE);

  private CardAcquisitionAllowList() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Returns whether a card may be offered through production acquisition routes.
   *
   * @param card card definition to evaluate
   * @return true when base and upgrade effects are all safe for live combat acquisition
   */
  public static boolean isAllowed(CardConfig card) {
    Objects.requireNonNull(card, "card cannot be null");
    if (card.id == null || card.id.isBlank()) {
      return false;
    }
    return !usesExcludedEffect(card.effects) && !usesExcludedUpgradeEffect(card.upgrade);
  }

  /**
   * Effects that are silently dropped in live combat and therefore must not be sold or rewarded.
   *
   * @return immutable view of excluded effect types
   */
  public static Set<EffectType> excludedLiveEffects() {
    return Set.copyOf(EXCLUDED_LIVE_EFFECTS);
  }

  private static boolean usesExcludedUpgradeEffect(CardUpgradeConfig upgrade) {
    return upgrade != null && usesExcludedEffect(upgrade.effects);
  }

  private static boolean usesExcludedEffect(EffectConfig[] effects) {
    if (effects == null) {
      return false;
    }
    for (EffectConfig effect : effects) {
      if (effect != null && effect.type != null && EXCLUDED_LIVE_EFFECTS.contains(effect.type)) {
        return true;
      }
    }
    return false;
  }
}

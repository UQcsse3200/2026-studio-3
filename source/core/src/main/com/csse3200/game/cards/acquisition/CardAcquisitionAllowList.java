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
 * <p>This is the shared place for future acquisition exclusions. It currently excludes nothing:
 * live {@code CardEffectHandler} already resolves {@link EffectType#SUNDER} and {@link
 * EffectType#PIERCE}. Member 5 acquisition and Member 2 post-battle rewards should share this list.
 */
public final class CardAcquisitionAllowList {
  private static final Set<EffectType> EXCLUDED_LIVE_EFFECTS = EnumSet.noneOf(EffectType.class);

  private CardAcquisitionAllowList() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Returns whether a card may be offered through production acquisition routes.
   *
   * @param card card definition to evaluate
   * @return true when the card has a usable id and does not use an excluded live effect
   */
  public static boolean isAllowed(CardConfig card) {
    Objects.requireNonNull(card, "card cannot be null");
    if (card.id == null || card.id.isBlank()) {
      return false;
    }
    return !usesExcludedEffect(card.effects) && !usesExcludedUpgradeEffect(card.upgrade);
  }

  /**
   * Effects that must not be sold or rewarded through production acquisition routes.
   *
   * @return immutable view of excluded effect types (empty until a future exclusion is added)
   */
  public static Set<EffectType> excludedLiveEffects() {
    return Set.copyOf(EXCLUDED_LIVE_EFFECTS);
  }

  private static boolean usesExcludedUpgradeEffect(CardUpgradeConfig upgrade) {
    return upgrade != null && usesExcludedEffect(upgrade.effects);
  }

  private static boolean usesExcludedEffect(EffectConfig[] effects) {
    if (effects == null || EXCLUDED_LIVE_EFFECTS.isEmpty()) {
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

package com.csse3200.game.cards.configs;

import com.csse3200.game.cards.Rarity;

/**
 * Values used when one runtime copy of a card is upgraded.
 *
 * <p>The upgraded variant inherits its ID, type, target and artwork from the containing {@link
 * CardConfig}. Keeping those shared fields on the base definition makes the relationship explicit
 * and allows the normal and upgraded variants to use the same dynamic Scene2D layout.
 */
public class CardUpgradeConfig {
  /** Name shown for the upgraded variant. */
  public String name = "";

  /** Rules text shown for the upgraded variant. */
  public String description = "";

  /** Energy required to play the upgraded variant. */
  public int cost = 0;

  /** Rarity shown for the upgraded variant. */
  public Rarity rarity = Rarity.COMMON;

  /** Effects applied in order when the upgraded variant is played. */
  public EffectConfig[] effects = new EffectConfig[0];

  /** Required by the JSON deserialiser. */
  public CardUpgradeConfig() {}
}

package com.csse3200.game.cards.configs;

import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.TargetType;

/**
 * Configuration for a single card, loaded from {@code configs/cards.json}. A card's category is
 * kept separate from its effects, so that any card type may carry any combination of effects. For
 * example, an ATTACK card may deal damage and also apply a debuff.
 */
public class CardConfig {
  /** Unique key used to look this card up. */
  public String id = "";

  /** Name shown to the player. */
  public String name = "";

  /** Rules text shown to the player. */
  public String description = "";

  /** Energy required to play this card. */
  public int cost = 0;

  /** Category of the card, kept separate from what the card actually does. */
  public CardType type = CardType.ATTACK;

  /** How rare the card is, used when offering cards as rewards. */
  public Rarity rarity = Rarity.COMMON;

  /** Who the card's effects are applied to. */
  public TargetType target = TargetType.SINGLE_ENEMY;

  /** Effects applied in order when the card is played. */
  public EffectConfig[] effects = new EffectConfig[0];

  /** Path to the card artwork, relative to the assets directory. */
  public String texturePath = "";

  /** Required by the JSON deserialiser. */
  public CardConfig() {}
}

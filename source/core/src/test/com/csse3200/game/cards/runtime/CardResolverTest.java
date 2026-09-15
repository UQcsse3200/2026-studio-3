package com.csse3200.game.cards.runtime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CardResolverTest {
  private final CardResolver resolver = new CardResolver();

  @Test
  void shouldResolveBaseAndUpgradedStrikeFromOfficialConfiguration() {
    CardConfig strike = strike();

    ResolvedCard base =
        resolver.resolve(
            strike, new CardInstance("strike-base", "strike", CardInstance.BASE_LEVEL));
    ResolvedCard upgraded =
        resolver.resolve(
            strike, new CardInstance("strike-upgrade", "strike", CardInstance.UPGRADED_LEVEL));

    assertAll(
        () -> assertEquals("Strike", base.name()),
        () -> assertEquals(1, base.cost()),
        () -> assertEquals(6, base.effects().get(0).value),
        () -> assertFalse(base.upgraded()),
        () -> assertEquals("Strike+", upgraded.name()),
        () -> assertEquals("Deal 12 damage.", upgraded.description()),
        () -> assertEquals(1, upgraded.cost()),
        () -> assertEquals(Rarity.COMMON, upgraded.rarity()),
        () -> assertEquals(12, upgraded.effects().get(0).value),
        () -> assertTrue(upgraded.upgraded()),
        () -> assertEquals(CardType.ATTACK, upgraded.type()),
        () -> assertEquals(TargetType.SINGLE_ENEMY, upgraded.target()),
        () -> assertEquals(base.texturePath(), upgraded.texturePath()),
        () -> assertEquals("images/cards/strike.png", upgraded.texturePath()));
  }

  @Test
  void shouldDeepCopyEffectsWithoutMutatingSourceConfiguration() {
    CardConfig strike = strike();
    EffectConfig sourceBaseEffect = strike.effects[0];
    EffectConfig sourceUpgradeEffect = strike.upgrade.effects[0];

    ResolvedCard upgraded =
        resolver.resolve(
            strike, new CardInstance("strike-upgrade", "strike", CardInstance.UPGRADED_LEVEL));

    assertEquals(6, sourceBaseEffect.value);
    assertEquals(12, sourceUpgradeEffect.value);
    assertThrows(
        UnsupportedOperationException.class,
        () -> upgraded.effects().add(new EffectConfig(EffectType.DAMAGE, 99)));

    List<EffectConfig> consumerCopy = upgraded.effects();
    consumerCopy.get(0).value = 99;
    strike.upgrade.effects[0].value = 20;

    assertEquals(12, upgraded.effects().get(0).value);
    assertEquals(6, strike.effects[0].value);
    assertEquals(20, strike.upgrade.effects[0].value);
  }

  @Test
  void shouldRejectNullArguments() {
    CardConfig strike = strike();
    CardInstance instance = new CardInstance("strike-base", "strike", CardInstance.BASE_LEVEL);

    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> resolver.resolve(null, instance)),
        () -> assertThrows(IllegalArgumentException.class, () -> resolver.resolve(strike, null)));
  }

  @Test
  void shouldRejectMismatchedCardIds() {
    CardInstance wrongInstance = new CardInstance("copy-1", "defend", CardInstance.BASE_LEVEL);

    assertThrows(IllegalArgumentException.class, () -> resolver.resolve(strike(), wrongInstance));
  }

  @Test
  void shouldRejectUpgradedInstanceWhenUpgradeDefinitionIsMissing() {
    CardConfig card = strike();
    card.upgrade = null;
    CardInstance upgraded = new CardInstance("copy-1", "strike", CardInstance.UPGRADED_LEVEL);

    assertThrows(IllegalStateException.class, () -> resolver.resolve(card, upgraded));
  }

  private static CardConfig strike() {
    return CardConfigLoader.loadCards().stream()
        .filter(card -> "strike".equals(card.id))
        .findFirst()
        .orElseThrow();
  }
}

package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import org.junit.jupiter.api.Test;

class CardConfigTest {
  @Test
  void shouldDefaultIdentifiersToEmptyStrings() {
    CardConfig card = new CardConfig();
    assertEquals("", card.id);
    assertEquals("", card.name);
  }

  @Test
  void shouldDefaultRemainingFields() {
    CardConfig card = new CardConfig();

    assertEquals("", card.description);
    assertEquals(0, card.cost);
    assertEquals(CardType.ATTACK, card.type);
    assertEquals(Rarity.COMMON, card.rarity);
    assertEquals(TargetType.SINGLE_ENEMY, card.target);
    assertNotNull(card.effects);
    assertEquals(0, card.effects.length);
    assertEquals("", card.texturePath);
  }

  @Test
  void shouldDeclareEffectsAsArray() throws NoSuchFieldException {
    // FileLoader's shared Json cannot resolve List<T> element types after generic erasure, so this
    // field must remain an EffectConfig array for reliable deserialisation.
    Class<?> effectsType = CardConfig.class.getDeclaredField("effects").getType();

    assertTrue(effectsType.isArray());
    assertEquals(EffectConfig[].class, effectsType);
  }

  @Test
  void shouldPreserveMultipleEffectsInDeclarationOrder() {
    CardConfig card = new CardConfig();
    EffectConfig damage = new EffectConfig(EffectType.DAMAGE, 4);
    EffectConfig poison = new EffectConfig(EffectType.POISON, 3, 2);

    card.effects = new EffectConfig[] {damage, poison};

    assertEquals(2, card.effects.length);
    assertSame(damage, card.effects[0]);
    assertSame(poison, card.effects[1]);
  }
}

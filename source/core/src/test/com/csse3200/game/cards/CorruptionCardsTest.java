package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CorruptionCardsTest {
  private CardLibrary library;

  @BeforeEach
  void setUp() {
    library = new CardLibrary(CardConfigLoader.loadCards());
  }

  @Test
  void shouldLoadPoisonFlask() {
    CardConfig card = library.getCard("poison_flask").orElseThrow();

    assertAll(
        () -> assertEquals("Poison Flask", card.name),
        () -> assertEquals("Apply 5 Poison for 3 turns.", card.description),
        () -> assertEquals(1, card.cost),
        () -> assertEquals(CardType.SKILL, card.type),
        () -> assertEquals(Rarity.UNCOMMON, card.rarity),
        () -> assertEquals(TargetType.SINGLE_ENEMY, card.target),
        () -> assertEquals(1, card.effects.length),
        () -> assertEffect(card.effects[0], EffectType.POISON, 5, 3),
        () -> assertEquals("images/cards/poison_flask.png", card.texturePath));
  }

  @Test
  void shouldLoadPoisonBlade() {
    CardConfig card = library.getCard("poison_blade").orElseThrow();

    assertAll(
        () -> assertEquals("Poison Blade", card.name),
        () -> assertEquals("Deal 10 damage. Apply 4 Poison for 2 turns.", card.description),
        () -> assertEquals(2, card.cost),
        () -> assertEquals(CardType.ATTACK, card.type),
        () -> assertEquals(Rarity.UNCOMMON, card.rarity),
        () -> assertEquals(TargetType.SINGLE_ENEMY, card.target),
        () -> assertEquals(2, card.effects.length),
        () -> assertEffect(card.effects[0], EffectType.DAMAGE, 10, 0),
        () -> assertEffect(card.effects[1], EffectType.POISON, 4, 2),
        () -> assertEquals("images/cards/poison_blade.png", card.texturePath));
  }

  @Test
  void shouldLoadPoisonCloud() {
    CardConfig card = library.getCard("poison_cloud").orElseThrow();

    assertAll(
        () -> assertEquals("Poison Cloud", card.name),
        () -> assertEquals("Apply 3 Poison to all enemies for 3 turns.", card.description),
        () -> assertEquals(2, card.cost),
        () -> assertEquals(CardType.SKILL, card.type),
        () -> assertEquals(Rarity.UNCOMMON, card.rarity),
        () -> assertEquals(TargetType.ALL_ENEMIES, card.target),
        () -> assertEquals(1, card.effects.length),
        () -> assertEffect(card.effects[0], EffectType.POISON, 3, 3),
        () -> assertEquals("images/cards/poison_cloud.png", card.texturePath));
  }

  @Test
  void shouldLoadPoisonMark() {
    CardConfig card = library.getCard("poison_mark").orElseThrow();

    assertAll(
        () -> assertEquals("Poison Mark", card.name),
        () ->
            assertEquals(
                "Apply 2 Vulnerable and 2 Poison to an enemy for 2 turns.", card.description),
        () -> assertEquals(1, card.cost),
        () -> assertEquals(CardType.SKILL, card.type),
        () -> assertEquals(Rarity.RARE, card.rarity),
        () -> assertEquals(TargetType.SINGLE_ENEMY, card.target),
        () -> assertEquals(2, card.effects.length),
        () -> assertEffect(card.effects[0], EffectType.VULNERABLE, 2, 2),
        () -> assertEffect(card.effects[1], EffectType.POISON, 2, 2),
        () -> assertEquals("images/cards/poison_mark.png", card.texturePath));

  }

  private static void assertEffect(EffectConfig effect, EffectType type, int value, int duration) {
    assertEquals(type, effect.type);
    assertEquals(value, effect.value);
    assertEquals(duration, effect.duration);
  }
}

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
  void shouldLoadVenomFlask() {
    CardConfig card = library.getCard("venom_flask").orElseThrow();

    assertAll(
        () -> assertEquals("Venom Flask", card.name),
        () -> assertEquals("Apply 5 Poison for 3 turns.", card.description),
        () -> assertEquals(1, card.cost),
        () -> assertEquals(CardType.SKILL, card.type),
        () -> assertEquals(Rarity.UNCOMMON, card.rarity),
        () -> assertEquals(TargetType.SINGLE_ENEMY, card.target),
        () -> assertEquals(1, card.effects.length),
        () -> assertEffect(card.effects[0], EffectType.POISON, 5, 3),
        () -> assertEquals("images/cards/poison_dagger.png", card.texturePath));
  }

  @Test
  void shouldLoadTaintedEdge() {
    CardConfig card = library.getCard("tainted_edge").orElseThrow();

    assertAll(
        () -> assertEquals("Tainted Edge", card.name),
        () -> assertEquals("Deal 10 damage. Apply 4 Poison for 2 turns.", card.description),
        () -> assertEquals(2, card.cost),
        () -> assertEquals(CardType.ATTACK, card.type),
        () -> assertEquals(Rarity.UNCOMMON, card.rarity),
        () -> assertEquals(TargetType.SINGLE_ENEMY, card.target),
        () -> assertEquals(2, card.effects.length),
        () -> assertEffect(card.effects[0], EffectType.DAMAGE, 10, 0),
        () -> assertEffect(card.effects[1], EffectType.POISON, 4, 2),
        () -> assertEquals("images/cards/poison_dagger.png", card.texturePath));
  }

  private static void assertEffect(EffectConfig effect, EffectType type, int value, int duration) {
    assertEquals(type, effect.type);
    assertEquals(value, effect.value);
    assertEquals(duration, effect.duration);
  }
}

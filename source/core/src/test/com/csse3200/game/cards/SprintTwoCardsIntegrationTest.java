package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Loads Sprint 2 cards from the production configuration and checks their library contracts. */
@ExtendWith(GameExtension.class)
class SprintTwoCardsIntegrationTest {
  private CardLibrary library;

  @BeforeEach
  void setUp() {
    library = new CardLibrary(CardConfigLoader.loadCards());
  }

  @Test
  void shouldLoadWardingSweepWithExpectedFields() {
    CardConfig card = library.getCard("warding_sweep").orElseThrow();

    assertAll(
        () -> assertEquals("warding_sweep", card.id),
        () -> assertEquals("Warding Sweep", card.name),
        () -> assertEquals("Deal 4 damage to all enemies.", card.description),
        () -> assertEquals(2, card.cost),
        () -> assertEquals(CardType.ATTACK, card.type),
        () -> assertEquals(Rarity.COMMON, card.rarity),
        () -> assertEquals(TargetType.ALL_ENEMIES, card.target),
        () -> assertEquals("images/cards/warding_sweep.png", card.texturePath));
    assertEquals(1, card.effects.length);
    assertAll(
        () -> assertEquals(EffectType.DAMAGE, card.effects[0].type),
        () -> assertEquals(4, card.effects[0].value),
        () -> assertEquals(0, card.effects[0].duration));
  }

  @Test
  void shouldValidateLoadedWardingSweep() {
    CardConfig card = library.getCard("warding_sweep").orElseThrow();

    assertTrue(CardValidator.validate(card).isEmpty());
  }
}

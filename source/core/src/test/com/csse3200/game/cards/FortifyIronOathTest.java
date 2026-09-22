package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.CardUpgradeConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Configuration checks for the Round 2 FORTIFY effect card. */
@ExtendWith(GameExtension.class)
class FortifyIronOathTest {
  private CardLibrary library;

  @BeforeEach
  void setUp() {
    library = new CardLibrary(CardConfigLoader.loadCards());
  }

  @Test
  void shouldLoadAndValidateIronOath() {
    CardConfig card = library.getCard("iron_oath").orElseThrow();

    assertTrue(CardValidator.isValid(card));
    assertTrue(CardValidator.isCompatibleWithTarget(EffectType.FORTIFY, TargetType.SELF));
  }

  @Test
  void shouldConfigureIronOathWithFortifyAndUpgrade() {
    CardConfig card = library.getCard("iron_oath").orElseThrow();
    CardUpgradeConfig upgrade = card.upgrade;

    assertAll(
        () -> assertEquals("Iron Oath", card.name),
        () -> assertEquals(1, card.cost),
        () -> assertEquals(CardType.SKILL, card.type),
        () -> assertEquals(Rarity.UNCOMMON, card.rarity),
        () -> assertEquals(TargetType.SELF, card.target),
        () -> assertEquals(1, card.effects.length),
        () -> assertEffect(card.effects[0], EffectType.FORTIFY, 4, 0),
        () -> assertEquals("images/cards/iron_oath.png", card.texturePath),
        () -> assertEquals("Iron Oath+", upgrade.name),
        () -> assertEquals(1, upgrade.cost),
        () -> assertEquals(Rarity.UNCOMMON, upgrade.rarity),
        () -> assertEquals(1, upgrade.effects.length),
        () -> assertEffect(upgrade.effects[0], EffectType.FORTIFY, 6, 0));
  }

  private static void assertEffect(EffectConfig effect, EffectType type, int value, int duration) {
    assertEquals(type, effect.type);
    assertEquals(value, effect.value);
    assertEquals(duration, effect.duration);
  }
}

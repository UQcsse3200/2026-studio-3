package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.CardUpgradeConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.effects.ResolvedCardEffect;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.cards.CardEffectHandler;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Configuration and live combat checks for the Round 2 FORTIFY effect card. */
@ExtendWith(GameExtension.class)
class FortifyIronOathTest {
  private CardLibrary library;
  private CardEffectHandler handler;
  private Entity player;
  private CombatStatsComponent stats;

  @BeforeEach
  void setUp() {
    library = new CardLibrary(CardConfigLoader.loadCards());
    handler = new CardEffectHandler();
    stats = new CombatStatsComponent(50, 0);
    player = new Entity().addComponent(stats);
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

  @Test
  void shouldAddArmourWithoutChangingBlockWhenPlayed() {
    stats.addBlock(3);

    handler.applyPlayerEffects(playedEffects("iron_oath"), player);

    assertEquals(4, stats.getArmour());
    assertEquals(3, stats.getBlock());
  }

  @Test
  void shouldKeepArmourWhenBlockResetsAtTurnStart() {
    handler.applyPlayerEffects(playedEffects("iron_oath"), player);
    handler.applyPlayerEffects(playedEffects("defend"), player);

    stats.resetBlock();

    assertEquals(0, stats.getBlock());
    assertEquals(4, stats.getArmour());
  }

  @Test
  void shouldAbsorbDamageWithBlockBeforeArmour() {
    handler.applyPlayerEffects(playedEffects("iron_oath"), player);
    stats.addBlock(5);

    stats.takeDamage(7);

    assertEquals(0, stats.getBlock());
    assertEquals(2, stats.getArmour());
    assertEquals(50, stats.getHealth());
  }

  private List<ResolvedCardEffect> playedEffects(String cardId) {
    CardConfig card = library.getCard(cardId).orElseThrow();
    List<ResolvedCardEffect> effects = new ArrayList<>();
    for (int i = 0; i < card.effects.length; i++) {
      EffectConfig effect = card.effects[i];
      effects.add(
          new ResolvedCardEffect(
              card.id, effect.type, card.target, effect.value, effect.duration, i));
    }
    return effects;
  }

  private static void assertEffect(EffectConfig effect, EffectType type, int value, int duration) {
    assertEquals(type, effect.type);
    assertEquals(value, effect.value);
    assertEquals(duration, effect.duration);
  }
}

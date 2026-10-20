package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.components.cards.CardUpgradeSelection;
import com.csse3200.game.components.cards.PlayerDeckCardUpgradeCommitter;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Checks production configuration through the campfire selection and persistent upgrade path. */
@ExtendWith(GameExtension.class)
class PoisonUpgradeIntegrationTest {
  @Test
  void shouldUpgradeFlask() {
    checkUpgrade("poison_flask", 15, 21);
  }

  @Test
  void shouldUpgradeCloud() {
    checkUpgrade("poison_cloud", 9, 12);
  }

  @Test
  void shouldUpgradeBlade() {
    checkUpgrade("poison_blade", 18, 22);
  }

  @Test
  void shouldUpgradeMark() {
    checkUpgrade("poison_mark", 4, 6);
  }

  private void checkUpgrade(String cardId, int baseDamage, int upgradedDamage) {
    CardService library = new CardLibrary(CardConfigLoader.loadCards());
    var config = library.getCard(cardId).orElseThrow();
    CardInstance first = new CardInstance("first", cardId, CardInstance.BASE_LEVEL);
    CardInstance second = new CardInstance("second", cardId, CardInstance.BASE_LEVEL);
    PlayerDeck deck = PlayerDeck.fromInstances(library, List.of(first, second));
    CardUpgradeSelection selection = CardUpgradeSelection.forPlayerDeck(deck, library, 1);

    assertEquals(2, selection.getCardUpgradeOption().size());
    var option = selection.getCardUpgradeOption().get(0);
    assertEquals(config.name + "+", option.preview().name());
    assertEquals(config.cost, option.preview().cost());
    assertEquals(config.target, option.preview().target());
    assertEquals(config.type, option.preview().type());
    assertEquals(config.rarity, option.preview().rarity());
    assertEquals(baseDamage, nominalDamage(option.current().effects()));
    assertEquals(upgradedDamage, nominalDamage(option.preview().effects()));
    assertEquals(
        option.current().effects().stream().map(effect -> effect.type).toList(),
        option.preview().effects().stream().map(effect -> effect.type).toList());
    assertEquals(
        option.current().effects().stream().map(effect -> effect.duration).toList(),
        option.preview().effects().stream().map(effect -> effect.duration).toList());

    selection.toggle("first");
    new PlayerDeckCardUpgradeCommitter(deck).commitUpgrades(selection.getSelectedInstanceIds());
    selection.refresh();

    assertTrue(deck.getCards().get(0).isUpgraded());
    assertFalse(deck.getCards().get(1).isUpgraded());
    assertEquals("first", deck.getCards().get(0).instanceId());
    assertEquals(
        List.of("second"),
        selection.getCardUpgradeOption().stream().map(o -> o.instance().instanceId()).toList());
    CardResolver resolver = new CardResolver();
    assertEquals(
        upgradedDamage, nominalDamage(resolver.resolve(config, deck.getCards().get(0)).effects()));
    assertEquals(
        baseDamage, nominalDamage(resolver.resolve(config, deck.getCards().get(1)).effects()));
  }

  // Nominal per-target damage assumes full poison resolution; Vulnerable is setup, not damage.
  private static int nominalDamage(List<com.csse3200.game.cards.configs.EffectConfig> effects) {
    return effects.stream()
        .mapToInt(
            effect ->
                switch (effect.type) {
                  case DAMAGE, PIERCE -> effect.value;
                  case POISON -> effect.value * effect.duration;
                  default -> 0;
                })
        .sum();
  }
}

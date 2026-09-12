package com.csse3200.game.cards.runtime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.CardUpgradeConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class CardInstanceFactoryTest {
  @Test
  void shouldCreateUniqueNormalInstancesForTheSameCard() {
    CardInstanceFactory factory = new CardInstanceFactory(library());

    CardInstance first = factory.create("strike");
    CardInstance second = factory.create("strike");

    assertAll(
        () -> assertNotEquals(first.instanceId(), second.instanceId()),
        () -> assertEquals("strike", first.cardId()),
        () -> assertEquals("strike", second.cardId()),
        () -> assertTrue(!first.isUpgraded()),
        () -> assertTrue(!second.isUpgraded()));
  }

  @Test
  void shouldCreateUpgradedInstanceWhenUpgradeExists() {
    CardInstance upgraded = new CardInstanceFactory(library()).createUpgraded("strike");

    assertEquals("strike", upgraded.cardId());
    assertTrue(upgraded.isUpgraded());
  }

  @Test
  void shouldRejectUnknownOrBlankCardIds() {
    CardInstanceFactory factory = new CardInstanceFactory(library());

    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> factory.create("missing")),
        () -> assertThrows(IllegalArgumentException.class, () -> factory.create(null)),
        () -> assertThrows(IllegalArgumentException.class, () -> factory.create(" ")));
  }

  @Test
  void shouldRejectUpgradedInstanceWhenUpgradeDefinitionIsMissing() {
    CardInstanceFactory factory = new CardInstanceFactory(library());

    assertThrows(IllegalArgumentException.class, () -> factory.createUpgraded("defend"));
  }

  @Test
  void shouldRejectNullCardService() {
    assertThrows(IllegalArgumentException.class, () -> new CardInstanceFactory(null));
  }

  private static CardLibrary library() {
    return new CardLibrary(List.of(card("strike", true), card("defend", false)));
  }

  private static CardConfig card(String id, boolean withUpgrade) {
    CardConfig card = new CardConfig();
    card.id = id;
    card.name = id;
    card.description = "Test card.";
    card.cost = 1;
    card.effects = new EffectConfig[] {new EffectConfig(EffectType.DAMAGE, 6)};
    card.texturePath = "images/cards/test.png";
    if (withUpgrade) {
      CardUpgradeConfig upgrade = new CardUpgradeConfig();
      upgrade.name = id + "+";
      upgrade.description = "Upgraded test card.";
      upgrade.cost = 1;
      upgrade.rarity = Rarity.COMMON;
      upgrade.effects = new EffectConfig[] {new EffectConfig(EffectType.DAMAGE, 12)};
      card.upgrade = upgrade;
    }
    return card;
  }
}

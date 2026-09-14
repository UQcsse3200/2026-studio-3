package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.csse3200.game.cards.configs.CardUpgradeConfig;
import org.junit.jupiter.api.Test;

class CardUpgradeConfigTest {
  @Test
  void shouldProvideDeserialisationSafeDefaults() {
    CardUpgradeConfig upgrade = new CardUpgradeConfig();

    assertEquals("", upgrade.name);
    assertEquals("", upgrade.description);
    assertEquals(0, upgrade.cost);
    assertEquals(Rarity.COMMON, upgrade.rarity);
    assertNotNull(upgrade.effects);
    assertEquals(0, upgrade.effects.length);
  }
}

package com.csse3200.game.cards.acquisition;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CardAcquisitionAllowListTest {
  private CardService cards;

  @BeforeEach
  void setUp() {
    cards = new CardLibrary(CardConfigLoader.loadCards());
  }

  @Test
  void shouldAllowCardsWithoutDroppedLiveEffects() {
    assertTrue(CardAcquisitionAllowList.isAllowed(require("iron_oath")));
    assertTrue(CardAcquisitionAllowList.isAllowed(require("sealed_pact")));
    assertTrue(CardAcquisitionAllowList.isAllowed(require("purify")));
    assertTrue(CardAcquisitionAllowList.isAllowed(require("strike")));
  }

  @Test
  void shouldExcludeCardsUsingSunderOrPierce() {
    assertFalse(CardAcquisitionAllowList.isAllowed(require("poison_blade")));
    assertFalse(CardAcquisitionAllowList.isAllowed(require("unseal_the_breach")));
  }

  @Test
  void shouldExposeExcludedLiveEffects() {
    assertTrue(
        CardAcquisitionAllowList.excludedLiveEffects()
            .containsAll(java.util.Set.of(EffectType.SUNDER, EffectType.PIERCE)));
  }

  private CardConfig require(String id) {
    return cards.getCard(id).orElseThrow();
  }
}

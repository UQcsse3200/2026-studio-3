package com.csse3200.game.cards.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.effects.CardEffectResolution;
import com.csse3200.game.cards.effects.CardEffectResolutionService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Confirms the debug dialog reflects real results from the shared {@link
 * CardEffectResolutionService} — this is the sharing contract the class's own javadoc requires:
 * callers must inject the same instance used to actually play cards, not a separate copy.
 */
class CardEffectDebugComponentTest {
  @Test
  void shouldExposeResolutionsFromASuccessfulCardPlayOnTheSharedService() {
    CardConfig strike =
        card("strike", TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));
    CardEffectResolutionService cardEffects =
        new CardEffectResolutionService(new CardLibrary(List.of(strike)));
    CardEffectDebugComponent debug = new CardEffectDebugComponent(cardEffects);

    // Simulates a real card play: resolve() on the exact same service instance the
    // dialog was constructed with, the same call CardPlayService.playCard() makes internally.
    cardEffects.resolve("strike");

    List<CardEffectResolution> resolutions = debug.getResolutions();
    assertEquals(1, resolutions.size());
    assertEquals("strike", resolutions.get(0).cardId());
  }

  @Test
  void shouldNotSeeResolutionsFromADifferentServiceInstance() {
    CardConfig strike =
        card("strike", TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));
    CardLibrary library = new CardLibrary(List.of(strike));
    CardEffectResolutionService dialogService = new CardEffectResolutionService(library);
    CardEffectResolutionService otherService = new CardEffectResolutionService(library);
    CardEffectDebugComponent debug = new CardEffectDebugComponent(dialogService);

    // A card played on a *different* service instance must not silently appear here -
    // this is exactly the failure mode flagged in review: two unrelated instances.
    otherService.resolve("strike");

    assertTrue(debug.getResolutions().isEmpty());
  }

  @Test
  void shouldRejectNullService() {
    assertThrows(IllegalArgumentException.class, () -> new CardEffectDebugComponent(null));
  }

  private static CardConfig card(String id, TargetType target, EffectConfig... effects) {
    CardConfig card = new CardConfig();
    card.id = id;
    card.name = id;
    card.description = "Test card";
    card.cost = 1;
    card.type = CardType.SKILL;
    card.rarity = Rarity.COMMON;
    card.target = target;
    card.effects = effects;
    card.texturePath = "images/cards/" + id + ".png";
    return card;
  }
}

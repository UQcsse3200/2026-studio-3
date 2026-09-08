package com.csse3200.game.cards.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class CardEffectResolutionServiceTest {
  @Test
  void shouldResolveRecordAndExposeEffectsForConsumers() {
    CardConfig innerFocus =
        card("inner_focus", TargetType.SELF, new EffectConfig(EffectType.STRENGTH, 2));
    CardConfig strike =
        card("strike", TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));
    CardEffectResolutionService service =
        new CardEffectResolutionService(new CardLibrary(List.of(innerFocus, strike)));

    service.resolve("inner_focus");
    CardEffectResolution strikeResolution = service.resolve("strike");

    assertEquals(
        List.of("inner_focus", "strike"),
        service.getResolutions().stream().map(CardEffectResolution::cardId).toList());
    assertEquals(
        List.of(
            new ResolvedCardEffect("inner_focus", EffectType.STRENGTH, TargetType.SELF, 2, 0, 0)),
        service.getPlayerEffects());
    assertEquals(
        List.of(
            new ResolvedCardEffect("strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 8, 0, 0)),
        service.getEnemyEffects());
    assertEquals(strikeResolution.effects(), service.getEffectsOfType(EffectType.DAMAGE));
  }

  @Test
  void shouldClearTurnResultsWithoutClearingCombatStrength() {
    CardConfig innerFocus =
        card("inner_focus", TargetType.SELF, new EffectConfig(EffectType.STRENGTH, 2));
    CardConfig strike =
        card("strike", TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));
    CardEffectResolutionService service =
        new CardEffectResolutionService(new CardLibrary(List.of(innerFocus, strike)));
    service.resolve("inner_focus");

    service.clearTurnResults();
    CardEffectResolution nextTurnStrike = service.resolve("strike");

    assertEquals(1, service.getResolutions().size());
    assertEquals(8, nextTurnStrike.enemyEffects().get(0).value());
  }

  @Test
  void shouldResolveWithExternalCombatContextAndRecordResult() {
    CardConfig strike =
        card("strike", TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));
    CardEffectResolutionService service = new CardEffectResolutionService(new CardLibrary());

    CardEffectResolution result = service.resolve(strike, new CardEffectResolutionContext(2, 1, 1));

    assertEquals(
        List.of(
            new ResolvedCardEffect("strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 9, 0, 0)),
        result.enemyEffects());
    assertEquals(List.of(result), service.getResolutions());
  }

  @Test
  void shouldNotRecordFailedResolutions() {
    CardEffectResolutionService service = new CardEffectResolutionService(new CardLibrary());

    assertThrows(IllegalArgumentException.class, () -> service.resolve("missing"));
    assertEquals(List.of(), service.getResolutions());
  }

  @Test
  void shouldRejectMissingDependencies() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new CardEffectResolutionService((com.csse3200.game.cards.CardService) null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardEffectResolutionService(null, new PlayerEffectState(), new TurnEffectStore()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardEffectResolutionService(new CardEffectResolver(), null, new TurnEffectStore()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardEffectResolutionService(
                new CardEffectResolver(), new PlayerEffectState(), null));
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

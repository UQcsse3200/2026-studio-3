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

class CardEffectResolverTest {
  @Test
  void shouldResolveTeamSixCardConfigEffectsInOrder() {
    CardConfig poisonDagger =
        card(
            "poison_dagger",
            TargetType.SINGLE_ENEMY,
            new EffectConfig(EffectType.DAMAGE, 4),
            new EffectConfig(EffectType.POISON, 3, 3));
    CardEffectResolver resolver = new CardEffectResolver();

    CardEffectResolution result = resolver.resolve(poisonDagger, new PlayerEffectState(2));

    assertEquals("poison_dagger", result.cardId());
    assertEquals(
        List.of(
            new ResolvedCardEffect(
                "poison_dagger", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 6, 0, 0),
            new ResolvedCardEffect(
                "poison_dagger", EffectType.POISON, TargetType.SINGLE_ENEMY, 3, 3, 1)),
        result.effects());
  }

  @Test
  void shouldResolveByCardIdThroughTeamSixCardService() {
    CardConfig strike =
        card("strike", TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));
    CardEffectResolver resolver = new CardEffectResolver(new CardLibrary(List.of(strike)));

    CardEffectResolution result = resolver.resolve("strike", new PlayerEffectState(3));

    assertEquals(
        List.of(
            new ResolvedCardEffect("strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 9, 0, 0)),
        result.enemyEffects());
  }

  @Test
  void shouldResolveByCardIdWithExternalCombatContext() {
    CardConfig strike =
        card("strike", TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));
    CardEffectResolver resolver = new CardEffectResolver(new CardLibrary(List.of(strike)));

    CardEffectResolution result =
        resolver.resolve("strike", new CardEffectResolutionContext(2, 1, 1));

    assertEquals(
        List.of(
            new ResolvedCardEffect("strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 9, 0, 0)),
        result.enemyEffects());
  }

  @Test
  void shouldReturnPlayerEffectsAndUpdateStrengthForLaterCards() {
    CardEffectResolver resolver = new CardEffectResolver();
    PlayerEffectState playerState = new PlayerEffectState();
    CardConfig innerFocus =
        card("inner_focus", TargetType.SELF, new EffectConfig(EffectType.STRENGTH, 2));
    CardConfig strike =
        card("strike", TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));

    CardEffectResolution focusResult = resolver.resolve(innerFocus, playerState);
    CardEffectResolution strikeResult = resolver.resolve(strike, playerState);

    assertEquals(
        List.of(
            new ResolvedCardEffect("inner_focus", EffectType.STRENGTH, TargetType.SELF, 2, 0, 0)),
        focusResult.playerEffects());
    assertEquals(2, playerState.getStrength());
    assertEquals(
        List.of(
            new ResolvedCardEffect("strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 8, 0, 0)),
        strikeResult.enemyEffects());
  }

  @Test
  void shouldSplitEnemyAndPlayerEffects() {
    CardEffectResolution enemyResolution =
        new CardEffectResolution(
            "strike",
            List.of(
                new ResolvedCardEffect(
                    "strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 6, 0, 0)));
    CardEffectResolution playerResolution =
        new CardEffectResolution(
            "defend",
            List.of(new ResolvedCardEffect("defend", EffectType.BLOCK, TargetType.SELF, 5, 0, 0)));

    assertEquals(1, enemyResolution.enemyEffects().size());
    assertEquals(0, enemyResolution.playerEffects().size());
    assertEquals(0, playerResolution.enemyEffects().size());
    assertEquals(1, playerResolution.playerEffects().size());
  }

  @Test
  void shouldRejectInvalidResolverInputs() {
    CardEffectResolver resolver = new CardEffectResolver();
    CardConfig strike =
        card("strike", TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));

    assertThrows(
        IllegalArgumentException.class, () -> resolver.resolve(strike, (PlayerEffectState) null));
    assertThrows(
        IllegalArgumentException.class,
        () -> resolver.resolve((CardConfig) null, new PlayerEffectState()));
    assertThrows(
        IllegalStateException.class, () -> resolver.resolve("strike", new PlayerEffectState()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardEffectResolver(new CardLibrary()).resolve("missing", new PlayerEffectState()));
    assertThrows(
        IllegalArgumentException.class,
        () -> resolver.resolve(strike, (CardEffectResolutionContext) null));
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

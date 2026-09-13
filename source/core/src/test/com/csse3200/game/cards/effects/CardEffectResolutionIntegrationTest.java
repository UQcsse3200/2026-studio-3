package com.csse3200.game.cards.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CardEffectResolutionIntegrationTest {
  @Test
  void shouldResolveTheInitialTeamSixCards() {
    CardLibrary library = new CardLibrary(CardConfigLoader.loadCards());
    CardEffectResolver resolver = new CardEffectResolver(library);
    PlayerEffectState playerState = new PlayerEffectState();

    CardEffectResolution strike = resolver.resolve("strike", playerState);
    CardEffectResolution defend = resolver.resolve("defend", playerState);
    CardEffectResolution poisonDagger = resolver.resolve("poison_dagger", playerState);
    CardEffectResolution expose = resolver.resolve("expose", playerState);
    CardEffectResolution innerFocus = resolver.resolve("inner_focus", playerState);
    CardEffectResolution bandage = resolver.resolve("bandage", playerState);
    CardEffectResolution empoweredStrike = resolver.resolve("strike", playerState);

    assertEquals(
        List.of(
            new ResolvedCardEffect("strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 6, 0, 0)),
        strike.enemyEffects());
    assertEquals(
        List.of(new ResolvedCardEffect("defend", EffectType.BLOCK, TargetType.SELF, 5, 0, 0)),
        defend.playerEffects());
    assertEquals(
        List.of(
            new ResolvedCardEffect(
                "poison_dagger", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 4, 0, 0),
            new ResolvedCardEffect(
                "poison_dagger", EffectType.POISON, TargetType.SINGLE_ENEMY, 3, 3, 1)),
        poisonDagger.enemyEffects());
    assertEquals(
        List.of(
            new ResolvedCardEffect(
                "expose", EffectType.VULNERABLE, TargetType.ALL_ENEMIES, 2, 2, 0)),
        expose.enemyEffects());
    assertEquals(
        List.of(
            new ResolvedCardEffect("inner_focus", EffectType.STRENGTH, TargetType.SELF, 2, 0, 0)),
        innerFocus.playerEffects());
    assertEquals(
        List.of(new ResolvedCardEffect("bandage", EffectType.HEAL, TargetType.SELF, 6, 0, 0)),
        bandage.playerEffects());
    assertEquals(
        List.of(
            new ResolvedCardEffect("strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 8, 0, 0)),
        empoweredStrike.enemyEffects());
  }
}

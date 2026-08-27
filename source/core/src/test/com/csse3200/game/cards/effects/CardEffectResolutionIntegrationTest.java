package com.csse3200.game.cards.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CardEffectResolutionIntegrationTest {
  @Test
  void shouldResolveFirstStageTeam5CardEffectsWithoutTeam6Classes() {
    CardEffectResolver resolver = new CardEffectResolver();
    RecordingCharacterEffectGateway player = new RecordingCharacterEffectGateway(5);

    CardEffectRequest request =
        new CardEffectRequest(
            "poison_dagger",
            TargetType.SINGLE_ENEMY,
            List.of(new CardEffect(EffectType.DAMAGE, 4), new CardEffect(EffectType.POISON, 3, 3)));

    List<ResolvedCardEffect> results = resolver.resolve(request, player);

    assertEquals(
        List.of(
            new ResolvedCardEffect(
                "poison_dagger", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 9, 0),
            new ResolvedCardEffect(
                "poison_dagger", EffectType.POISON, TargetType.SINGLE_ENEMY, 3, 3)),
        results);
    assertEquals(List.of(), player.events);
  }
}

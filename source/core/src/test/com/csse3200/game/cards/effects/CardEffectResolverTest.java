package com.csse3200.game.cards.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class CardEffectResolverTest {
  private final CardEffectResolver resolver = new CardEffectResolver();

  @Test
  void shouldResolveEnemyEffectsInCardOrder() {
    RecordingCharacterEffectGateway player = new RecordingCharacterEffectGateway(2);
    CardEffectRequest request =
        new CardEffectRequest(
            "poison_dagger",
            TargetType.SINGLE_ENEMY,
            List.of(new CardEffect(EffectType.DAMAGE, 4), new CardEffect(EffectType.POISON, 3, 3)));

    List<ResolvedCardEffect> results = resolver.resolve(request, player);

    assertEquals(
        List.of(
            new ResolvedCardEffect(
                "poison_dagger", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 6, 0),
            new ResolvedCardEffect(
                "poison_dagger", EffectType.POISON, TargetType.SINGLE_ENEMY, 3, 3)),
        results);
    assertEquals(List.of(), player.events);
  }

  @Test
  void shouldApplySelfEffectsThroughPlayerGateway() {
    RecordingCharacterEffectGateway player = new RecordingCharacterEffectGateway();

    List<ResolvedCardEffect> results =
        resolver.resolve(
            "inner_focus",
            TargetType.SELF,
            List.of(
                new CardEffect(EffectType.BLOCK, 5),
                new CardEffect(EffectType.HEAL, 3),
                new CardEffect(EffectType.STRENGTH, 2)),
            player);

    assertEquals(List.of(), results);
    assertEquals(List.of("BLOCK:5", "HEAL:3", "STRENGTH:2"), player.events);
    assertEquals(5, player.block);
    assertEquals(3, player.healing);
    assertEquals(2, player.strength);
  }

  @Test
  void shouldResolveAllEnemiesAsOneCombatInstruction() {
    CardEffectRequest request =
        new CardEffectRequest(
            "battle_cry",
            TargetType.ALL_ENEMIES,
            List.of(new CardEffect(EffectType.VULNERABLE, 2, 2)));

    List<ResolvedCardEffect> results =
        resolver.resolve(request, new RecordingCharacterEffectGateway());

    assertEquals(
        List.of(
            new ResolvedCardEffect(
                "battle_cry", EffectType.VULNERABLE, TargetType.ALL_ENEMIES, 2, 2)),
        results);
  }

  @Test
  void shouldRejectMissingRequestOrPlayerGateway() {
    assertThrows(
        IllegalArgumentException.class,
        () -> resolver.resolve(null, new RecordingCharacterEffectGateway()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            resolver.resolve(
                new CardEffectRequest(
                    "strike",
                    TargetType.SINGLE_ENEMY,
                    List.of(new CardEffect(EffectType.DAMAGE, 6))),
                null));
  }

  @Test
  void shouldRejectInvalidRequests() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardEffectRequest(
                "", TargetType.SINGLE_ENEMY, List.of(new CardEffect(EffectType.DAMAGE, 6))));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CardEffectRequest("strike", null, List.of(new CardEffect(EffectType.DAMAGE, 6))));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CardEffectRequest("strike", TargetType.SINGLE_ENEMY, List.of()));
  }
}

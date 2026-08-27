package com.csse3200.game.cards.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EffectExecutorTest {
  private EffectExecutor executor;

  @BeforeEach
  void setUp() {
    executor = new EffectExecutor();
  }

  @Test
  void shouldResolveEnemyDamageWithPlayerStrength() {
    RecordingCharacterEffectGateway player = new RecordingCharacterEffectGateway(5);

    List<ResolvedCardEffect> results =
        executor.execute(
            "strike", new CardEffect(EffectType.DAMAGE, 6), TargetType.SINGLE_ENEMY, player);

    assertEquals(
        List.of(
            new ResolvedCardEffect("strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 11, 0)),
        results);
    assertEquals(List.of(), player.events);
  }

  @Test
  void shouldClampOutgoingDamageAtZero() {
    RecordingCharacterEffectGateway player = new RecordingCharacterEffectGateway(-10);

    List<ResolvedCardEffect> results =
        executor.execute(
            "weakened_strike",
            new CardEffect(EffectType.DAMAGE, 6),
            TargetType.SINGLE_ENEMY,
            player);

    assertEquals(
        List.of(
            new ResolvedCardEffect(
                "weakened_strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 0, 0)),
        results);
  }

  @Test
  void shouldReturnEnemyStatusesForCombatSystemsToApply() {
    RecordingCharacterEffectGateway player = new RecordingCharacterEffectGateway();

    assertEquals(
        List.of(new ResolvedCardEffect("toxin", EffectType.POISON, TargetType.ALL_ENEMIES, 3, 2)),
        executor.execute(
            "toxin", new CardEffect(EffectType.POISON, 3, 2), TargetType.ALL_ENEMIES, player));
    assertEquals(
        List.of(
            new ResolvedCardEffect("expose", EffectType.VULNERABLE, TargetType.SINGLE_ENEMY, 2, 1)),
        executor.execute(
            "expose",
            new CardEffect(EffectType.VULNERABLE, 2, 1),
            TargetType.SINGLE_ENEMY,
            player));
    assertEquals(List.of(), player.events);
  }

  @Test
  void shouldApplySelfEffectsToPlayerGateway() {
    RecordingCharacterEffectGateway player = new RecordingCharacterEffectGateway();

    assertEquals(
        List.of(),
        executor.execute("defend", new CardEffect(EffectType.BLOCK, 5), TargetType.SELF, player));
    assertEquals(
        List.of(),
        executor.execute("bandage", new CardEffect(EffectType.HEAL, 4), TargetType.SELF, player));
    assertEquals(
        List.of(),
        executor.execute("focus", new CardEffect(EffectType.STRENGTH, 2), TargetType.SELF, player));

    assertEquals(List.of("BLOCK:5", "HEAL:4", "STRENGTH:2"), player.events);
  }

  @Test
  void shouldRejectUnsupportedTargetCombinations() {
    RecordingCharacterEffectGateway player = new RecordingCharacterEffectGateway();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            executor.execute(
                "burn", new CardEffect(EffectType.DAMAGE, 1), TargetType.SELF, player));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            executor.execute(
                "enemy_heal", new CardEffect(EffectType.HEAL, 1), TargetType.SINGLE_ENEMY, player));
  }

  @Test
  void shouldRejectInvalidArguments() {
    RecordingCharacterEffectGateway player = new RecordingCharacterEffectGateway();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            executor.execute(
                "", new CardEffect(EffectType.DAMAGE, 1), TargetType.SINGLE_ENEMY, player));
    assertThrows(
        IllegalArgumentException.class,
        () -> executor.execute("strike", null, TargetType.SINGLE_ENEMY, player));
    assertThrows(
        IllegalArgumentException.class,
        () -> executor.execute("strike", new CardEffect(EffectType.DAMAGE, 1), null, player));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            executor.execute(
                "strike", new CardEffect(EffectType.DAMAGE, 1), TargetType.SINGLE_ENEMY, null));
  }

  @Test
  void shouldValidateRawEffects() {
    assertThrows(IllegalArgumentException.class, () -> new CardEffect(null, 1));
    assertThrows(IllegalArgumentException.class, () -> new CardEffect(EffectType.DAMAGE, 0));
    assertThrows(IllegalArgumentException.class, () -> new CardEffect(EffectType.DAMAGE, 1, 1));
    assertThrows(IllegalArgumentException.class, () -> new CardEffect(EffectType.POISON, 1));
  }
}

package com.csse3200.game.cards.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.EffectConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EffectExecutorTest {
  private EffectExecutor executor;
  private PlayerEffectState playerState;

  @BeforeEach
  void setUp() {
    executor = new EffectExecutor();
    playerState = new PlayerEffectState();
  }

  @Test
  void shouldResolveDamageTargetingEnemyWithStrengthModifier() {
    playerState.addStrength(5);

    ResolvedCardEffect result =
        executor.resolve(
            "strike",
            new EffectConfig(EffectType.DAMAGE, 6),
            TargetType.SINGLE_ENEMY,
            0,
            playerState);

    assertEquals(
        new ResolvedCardEffect("strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 11, 0, 0),
        result);
  }

  @Test
  void shouldClampOutgoingDamageAtZero() {
    PlayerEffectState weakenedPlayer = new PlayerEffectState(-10);

    ResolvedCardEffect result =
        executor.resolve(
            "strike",
            new EffectConfig(EffectType.DAMAGE, 6),
            TargetType.SINGLE_ENEMY,
            0,
            weakenedPlayer);

    assertEquals(
        new ResolvedCardEffect("strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 0, 0, 0),
        result);
  }

  @Test
  void shouldApplyExternalStrengthFeebleAndVulnerableOnce() {
    CardEffectResolutionContext context = new CardEffectResolutionContext(2, 1, 1);

    ResolvedCardEffect result =
        executor.resolve(
            "strike", new EffectConfig(EffectType.DAMAGE, 6), TargetType.SINGLE_ENEMY, 0, context);

    assertEquals(
        new ResolvedCardEffect("strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 9, 0, 0),
        result);
  }

  @Test
  void shouldRoundModifiedDamageDown() {
    CardEffectResolutionContext context = new CardEffectResolutionContext(0, 1, 1);

    ResolvedCardEffect result =
        executor.resolve(
            "strike", new EffectConfig(EffectType.DAMAGE, 5), TargetType.SINGLE_ENEMY, 0, context);

    assertEquals(5, result.value());
  }

  @Test
  void shouldReturnStrengthWithoutMutatingStateWhenResolvingFromExternalContext() {
    CardEffectResolutionContext context = new CardEffectResolutionContext(0, 0, 0);

    ResolvedCardEffect result =
        executor.resolve(
            "inner_focus", new EffectConfig(EffectType.STRENGTH, 2), TargetType.SELF, 0, context);

    assertEquals(
        new ResolvedCardEffect("inner_focus", EffectType.STRENGTH, TargetType.SELF, 2, 0, 0),
        result);
    assertEquals(0, playerState.getStrength());
  }

  @Test
  void shouldReturnEveryOngoingEnemyStatusForOtherSystemsToApply() {
    int sequence = 0;
    for (EffectType effectType : EffectType.values()) {
      if (!effectType.usesDuration()) {
        continue;
      }

      String cardId = effectType.name().toLowerCase();
      assertEquals(
          new ResolvedCardEffect(cardId, effectType, TargetType.SINGLE_ENEMY, 3, 2, sequence),
          executor.resolve(
              cardId,
              new EffectConfig(effectType, 3, 2),
              TargetType.SINGLE_ENEMY,
              sequence,
              playerState));
      sequence++;
    }
  }

  @Test
  void shouldReturnPlayerEffectsWithoutApplyingExternalPlayerState() {
    assertEquals(
        new ResolvedCardEffect("defend", EffectType.BLOCK, TargetType.SELF, 5, 0, 0),
        executor.resolve(
            "defend", new EffectConfig(EffectType.BLOCK, 5), TargetType.SELF, 0, playerState));
    assertEquals(
        new ResolvedCardEffect("bandage", EffectType.HEAL, TargetType.SELF, 4, 0, 1),
        executor.resolve(
            "bandage", new EffectConfig(EffectType.HEAL, 4), TargetType.SELF, 1, playerState));
  }

  @Test
  void shouldUpdateTeamFiveStrengthStateWhenResolvingStrength() {
    ResolvedCardEffect result =
        executor.resolve(
            "inner_focus",
            new EffectConfig(EffectType.STRENGTH, 2),
            TargetType.SELF,
            0,
            playerState);

    assertEquals(
        new ResolvedCardEffect("inner_focus", EffectType.STRENGTH, TargetType.SELF, 2, 0, 0),
        result);
    assertEquals(2, playerState.getStrength());
  }

  @Test
  void shouldRejectUnsupportedTargetCombinations() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            executor.resolve(
                "self_damage",
                new EffectConfig(EffectType.DAMAGE, 1),
                TargetType.SELF,
                0,
                playerState));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            executor.resolve(
                "enemy_heal",
                new EffectConfig(EffectType.HEAL, 1),
                TargetType.SINGLE_ENEMY,
                0,
                playerState));
  }

  @Test
  void shouldRejectInvalidArguments() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            executor.resolve(
                "",
                new EffectConfig(EffectType.DAMAGE, 1),
                TargetType.SINGLE_ENEMY,
                0,
                playerState));
    assertThrows(
        IllegalArgumentException.class,
        () -> executor.resolve("strike", null, TargetType.SINGLE_ENEMY, 0, playerState));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            executor.resolve(
                "strike", new EffectConfig(EffectType.DAMAGE, 1), null, 0, playerState));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            executor.resolve(
                "strike",
                new EffectConfig(EffectType.DAMAGE, 1),
                TargetType.SINGLE_ENEMY,
                -1,
                playerState));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            executor.resolve(
                "strike",
                new EffectConfig(EffectType.DAMAGE, 1),
                TargetType.SINGLE_ENEMY,
                0,
                (PlayerEffectState) null));
  }

  @Test
  void shouldValidateEffectConfigValues() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            executor.resolve(
                "bad", new EffectConfig(null, 1), TargetType.SINGLE_ENEMY, 0, playerState));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            executor.resolve(
                "bad",
                new EffectConfig(EffectType.DAMAGE, 0),
                TargetType.SINGLE_ENEMY,
                0,
                playerState));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            executor.resolve(
                "bad",
                new EffectConfig(EffectType.DAMAGE, 1, 1),
                TargetType.SINGLE_ENEMY,
                0,
                playerState));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            executor.resolve(
                "bad",
                new EffectConfig(EffectType.POISON, 1),
                TargetType.SINGLE_ENEMY,
                0,
                playerState));
  }
}

package com.csse3200.game.cards.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.configs.EffectConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EffectExecutorTest {
  private EffectExecutor executor;
  private RecordingCharacterEffectGateway target;

  @BeforeEach
  void setUp() {
    executor = new EffectExecutor();
    target = new RecordingCharacterEffectGateway();
  }

  @Test
  void shouldExecuteEveryTeam6EffectType() {
    executor.execute(new EffectConfig(EffectType.DAMAGE, 6), target);
    executor.execute(new EffectConfig(EffectType.BLOCK, 5), target);
    executor.execute(new EffectConfig(EffectType.HEAL, 4), target);
    executor.execute(new EffectConfig(EffectType.POISON, 3, 2), target);
    executor.execute(new EffectConfig(EffectType.VULNERABLE, 2, 1), target);
    executor.execute(new EffectConfig(EffectType.STRENGTH, 2), target);

    assertEquals(6, target.damage);
    assertEquals(5, target.block);
    assertEquals(4, target.healing);
    assertEquals(3, target.poison);
    assertEquals(2, target.poisonDuration);
    assertEquals(2, target.vulnerable);
    assertEquals(1, target.vulnerableDuration);
    assertEquals(2, target.strength);
    assertEquals(
        List.of("DAMAGE:6", "BLOCK:5", "HEAL:4", "POISON:3:2", "VULNERABLE:2:1", "STRENGTH:2"),
        target.events);
  }

  @Test
  void shouldRejectMissingEffectOrTarget() {
    assertThrows(IllegalArgumentException.class, () -> executor.execute(null, target));
    assertThrows(
        IllegalArgumentException.class,
        () -> executor.execute(new EffectConfig(EffectType.DAMAGE, 1), null));
  }

  @Test
  void shouldRejectNullTypeAndNonPositiveValue() {
    EffectConfig nullType = new EffectConfig(EffectType.DAMAGE, 1);
    nullType.type = null;

    assertThrows(IllegalArgumentException.class, () -> executor.execute(nullType, target));
    assertThrows(
        IllegalArgumentException.class,
        () -> executor.execute(new EffectConfig(EffectType.DAMAGE, 0), target));
    assertThrows(
        IllegalArgumentException.class,
        () -> executor.execute(new EffectConfig(EffectType.DAMAGE, -1), target));
  }

  @Test
  void shouldEnforceTeam6DurationContract() {
    assertThrows(
        IllegalArgumentException.class,
        () -> executor.execute(new EffectConfig(EffectType.POISON, 3), target));
    assertThrows(
        IllegalArgumentException.class,
        () -> executor.execute(new EffectConfig(EffectType.DAMAGE, 6, 2), target));
  }
}

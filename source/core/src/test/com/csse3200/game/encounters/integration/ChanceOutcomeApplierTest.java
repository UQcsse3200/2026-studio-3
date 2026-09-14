package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChanceOutcomeApplierTest {
  @Test
  void shouldApplyCombinedHealthAndCurrencyOutcome() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 40);
    ChanceResolution result = new ChanceOutcomeApplier(player).apply(new ChanceOutcome(-10, 25));

    assertTrue(result.isSuccess());
    assertEquals(90, player.getHealth());
    assertEquals(65, player.getCurrency());
    assertEquals(100, result.getHealthBefore());
    assertEquals(90, result.getHealthAfter());
  }

  @Test
  void shouldRejectUnaffordableOutcomeWithoutPartialHealthChange() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(70, 5);
    ChanceResolution result = new ChanceOutcomeApplier(player).apply(new ChanceOutcome(20, -10));

    assertFalse(result.isSuccess());
    assertEquals(ChanceResolution.Status.INSUFFICIENT_CURRENCY, result.getStatus());
    assertEquals(70, player.getHealth());
    assertEquals(5, player.getCurrency());
    assertTrue(player.getMutations().isEmpty());
  }

  @Test
  void shouldClampLethalHealthOutcomeToZero() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(5, 10);
    ChanceResolution result = new ChanceOutcomeApplier(player).apply(new ChanceOutcome(-20, 0));

    assertTrue(result.isSuccess());
    assertEquals(0, player.getHealth());
  }

  @Test
  void shouldApplyDirectHealing() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(60, 100, 10);

    ChanceResolution result = new ChanceOutcomeApplier(player).apply(new ChanceOutcome(20, 0));

    assertTrue(result.isSuccess());
    assertEquals(80, player.getHealth());
  }

  @Test
  void shouldAcceptHealingClampedAtMaximumHealth() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(95, 100, 10);

    ChanceResolution result = new ChanceOutcomeApplier(player).apply(new ChanceOutcome(20, 0));

    assertTrue(result.isSuccess());
    assertEquals(100, player.getHealth());
    assertEquals(100, result.getHealthAfter());
  }

  @Test
  void shouldRejectArithmeticOverflowWithoutChangingPlayer() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, Integer.MAX_VALUE);
    ChanceResolution result = new ChanceOutcomeApplier(player).apply(new ChanceOutcome(0, 1));

    assertEquals(ChanceResolution.Status.ARITHMETIC_OVERFLOW, result.getStatus());
    assertEquals(Integer.MAX_VALUE, player.getCurrency());
    assertEquals(100, player.getHealth());
  }

  @Test
  void shouldNotApplyHealthWhenCurrencyUpdateFails() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 40);
    player.failNextCurrencyUpdate();

    ChanceResolution result = new ChanceOutcomeApplier(player).apply(new ChanceOutcome(-10, 5));

    assertEquals(ChanceResolution.Status.PLAYER_UPDATE_FAILED, result.getStatus());
    assertEquals(100, player.getHealth());
    assertEquals(40, player.getCurrency());
    assertFalse(player.getMutations().contains("health"));
  }

  @Test
  void shouldNotRollbackAfterDirectHealthUpdateIsRejected() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 40);
    player.rejectNextHealthUpdate();

    ChanceResolution result = new ChanceOutcomeApplier(player).apply(new ChanceOutcome(-10, 5));

    assertEquals(ChanceResolution.Status.ROLLBACK_FAILED, result.getStatus());
    assertEquals(100, player.getHealth());
    assertEquals(45, player.getCurrency());
  }

  @Test
  void shouldReportRollbackFailureSeparately() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 40);
    player.failAllCurrencyUpdates();

    ChanceResolution result = new ChanceOutcomeApplier(player).apply(new ChanceOutcome(-10, 5));

    assertEquals(ChanceResolution.Status.ROLLBACK_FAILED, result.getStatus());
    assertEquals(100, player.getHealth());
    assertEquals(40, player.getCurrency());
  }

  @Test
  void shouldCommitCurrencyBeforeApplyingDirectHealth() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 40);

    ChanceResolution result = new ChanceOutcomeApplier(player).apply(new ChanceOutcome(-10, 5));

    assertTrue(result.isSuccess());
    assertEquals(List.of("currency", "health"), player.getMutations());
    assertEquals(90, player.getHealth());
    assertEquals(45, player.getCurrency());
  }
}

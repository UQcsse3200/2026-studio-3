package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.encounters.integration.mocks.MockCardCatalogGateway;
import com.csse3200.game.encounters.integration.mocks.MockDeckGateway;
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
  void shouldRollbackCurrencyAfterDirectHealthUpdateIsRejected() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 40);
    player.rejectNextHealthUpdate();

    ChanceResolution result = new ChanceOutcomeApplier(player).apply(new ChanceOutcome(-10, 5));

    assertEquals(ChanceResolution.Status.PLAYER_UPDATE_FAILED, result.getStatus());
    assertEquals(100, player.getHealth());
    assertEquals(40, player.getCurrency());
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

  @Test
  void shouldApplyCardOnlyReward() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 40);
    MockDeckGateway deck = new MockDeckGateway();

    ChanceResolution result =
        createCardApplier(player, deck).apply(new ChanceOutcome(0, 0, "bandage"));

    assertTrue(result.isSuccess());
    assertEquals(List.of("bandage"), deck.getCardIds());
    assertEquals(100, player.getHealth());
    assertEquals(40, player.getCurrency());
  }

  @Test
  void shouldAllowDuplicateCardRewards() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 40);
    MockDeckGateway deck = new MockDeckGateway();
    deck.addExistingCard("bandage");

    ChanceResolution result =
        createCardApplier(player, deck).apply(new ChanceOutcome(0, 0, "bandage"));

    assertTrue(result.isSuccess());
    assertEquals(List.of("bandage", "bandage"), deck.getCardIds());
  }

  @Test
  void shouldApplyTwoCardRewardsIncludingDuplicateIds() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 40);
    MockDeckGateway deck = new MockDeckGateway();

    ChanceResolution result =
        createCardApplier(player, deck)
            .apply(ChanceOutcome.withCardRewards(0, 15, List.of("bandage", "bandage")));

    assertTrue(result.isSuccess());
    assertEquals(55, player.getCurrency());
    assertEquals(List.of("bandage", "bandage"), deck.getCardIds());
  }

  @Test
  void shouldRollbackEveryNewCardWithoutRemovingExistingDuplicatesWhenCurrencyFails() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 40);
    player.failNextCurrencyUpdate();
    MockDeckGateway deck = new MockDeckGateway();
    deck.addExistingCard("bandage");

    ChanceResolution result =
        createCardApplier(player, deck)
            .apply(ChanceOutcome.withCardRewards(0, 15, List.of("bandage", "bandage")));

    assertEquals(ChanceResolution.Status.PLAYER_UPDATE_FAILED, result.getStatus());
    assertEquals(40, player.getCurrency());
    assertEquals(List.of("bandage"), deck.getCardIds());
  }

  @Test
  void shouldRejectUnknownCardWithoutMutation() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 40);
    MockDeckGateway deck = new MockDeckGateway();
    ChanceOutcomeApplier applier =
        new ChanceOutcomeApplier(player, new MockCardCatalogGateway("bandage"), deck);

    ChanceResolution result = applier.apply(new ChanceOutcome(-10, 5, "missing"));

    assertEquals(ChanceResolution.Status.CARD_NOT_FOUND, result.getStatus());
    assertTrue(deck.getCardIds().isEmpty());
    assertEquals(100, player.getHealth());
    assertEquals(40, player.getCurrency());
  }

  @Test
  void shouldRejectFailedCardAddWithoutMutation() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 40);
    MockDeckGateway deck = new MockDeckGateway();
    deck.setFailAdd(true);

    ChanceResolution result =
        createCardApplier(player, deck).apply(new ChanceOutcome(-10, 5, "bandage"));

    assertEquals(ChanceResolution.Status.CARD_ADD_FAILED, result.getStatus());
    assertTrue(deck.getCardIds().isEmpty());
    assertEquals(100, player.getHealth());
    assertEquals(40, player.getCurrency());
  }

  @Test
  void shouldApplyCardAndCurrencyReward() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 40);
    MockDeckGateway deck = new MockDeckGateway();

    ChanceResolution result =
        createCardApplier(player, deck).apply(new ChanceOutcome(0, 15, "bandage"));

    assertTrue(result.isSuccess());
    assertEquals(List.of("bandage"), deck.getCardIds());
    assertEquals(55, player.getCurrency());
  }

  @Test
  void shouldApplyCardAndHealthReward() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(70, 100, 40);
    MockDeckGateway deck = new MockDeckGateway();

    ChanceResolution result =
        createCardApplier(player, deck).apply(new ChanceOutcome(20, 0, "bandage"));

    assertTrue(result.isSuccess());
    assertEquals(List.of("bandage"), deck.getCardIds());
    assertEquals(90, player.getHealth());
  }

  @Test
  void shouldApplyCardCurrencyAndHealthReward() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(70, 100, 40);
    MockDeckGateway deck = new MockDeckGateway();

    ChanceResolution result =
        createCardApplier(player, deck).apply(new ChanceOutcome(-10, 15, "bandage"));

    assertTrue(result.isSuccess());
    assertEquals(List.of("bandage"), deck.getCardIds());
    assertEquals(60, player.getHealth());
    assertEquals(55, player.getCurrency());
  }

  @Test
  void shouldRollbackCardWhenHealthIsRejectedBeforeMutation() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(70, 100, 40);
    player.rejectNextHealthUpdate();
    MockDeckGateway deck = new MockDeckGateway();

    ChanceResolution result =
        createCardApplier(player, deck).apply(new ChanceOutcome(20, 0, "bandage"));

    assertEquals(ChanceResolution.Status.PLAYER_UPDATE_FAILED, result.getStatus());
    assertTrue(deck.getCardIds().isEmpty());
    assertEquals(70, player.getHealth());
    assertEquals(40, player.getCurrency());
  }

  @Test
  void shouldRollbackCardAndCurrencyWhenHealthIsRejectedBeforeMutation() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(70, 100, 40);
    player.rejectNextHealthUpdate();
    MockDeckGateway deck = new MockDeckGateway();

    ChanceResolution result =
        createCardApplier(player, deck).apply(new ChanceOutcome(20, 15, "bandage"));

    assertEquals(ChanceResolution.Status.PLAYER_UPDATE_FAILED, result.getStatus());
    assertTrue(deck.getCardIds().isEmpty());
    assertEquals(70, player.getHealth());
    assertEquals(40, player.getCurrency());
  }

  @Test
  void shouldRejectUnaffordableCardOutcomeBeforeAddingCard() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(70, 100, 5);
    MockDeckGateway deck = new MockDeckGateway();
    deck.addExistingCard("bandage");

    ChanceResolution result =
        createCardApplier(player, deck).apply(new ChanceOutcome(20, -10, "bandage"));

    assertEquals(ChanceResolution.Status.INSUFFICIENT_CURRENCY, result.getStatus());
    assertEquals(List.of("bandage"), deck.getCardIds());
    assertEquals(70, player.getHealth());
    assertEquals(5, player.getCurrency());
  }

  @Test
  void shouldRollbackOnlyCardAddedByOutcomeWhenCurrencyFails() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 40);
    player.failNextCurrencyUpdate();
    MockDeckGateway deck = new MockDeckGateway();
    deck.addExistingCard("bandage");
    deck.addExistingCard("strike");

    ChanceResolution result =
        createCardApplier(player, deck).apply(new ChanceOutcome(-10, 5, "bandage"));

    assertEquals(ChanceResolution.Status.PLAYER_UPDATE_FAILED, result.getStatus());
    assertEquals(List.of("bandage", "strike"), deck.getCardIds());
    assertEquals(100, player.getHealth());
    assertEquals(40, player.getCurrency());
  }

  @Test
  void shouldReportCardRollbackFailureWithoutApplyingHealth() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 40);
    player.failNextCurrencyUpdate();
    MockDeckGateway deck = new MockDeckGateway();
    deck.setFailRemove(true);

    ChanceResolution result =
        createCardApplier(player, deck).apply(new ChanceOutcome(-10, 5, "bandage"));

    assertEquals(ChanceResolution.Status.ROLLBACK_FAILED, result.getStatus());
    assertEquals(List.of("bandage"), deck.getCardIds());
    assertEquals(100, player.getHealth());
    assertEquals(40, player.getCurrency());
  }

  private ChanceOutcomeApplier createCardApplier(
      MockPlayerStateGateway player, MockDeckGateway deck) {
    return new ChanceOutcomeApplier(player, new MockCardCatalogGateway("bandage", "strike"), deck);
  }
}

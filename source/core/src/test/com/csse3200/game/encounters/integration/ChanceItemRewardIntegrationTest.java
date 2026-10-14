package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.csse3200.game.chance.ChanceChoice;
import com.csse3200.game.chance.ChanceEncounter;
import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.encounters.integration.mocks.MockCardCatalogGateway;
import com.csse3200.game.encounters.integration.mocks.MockDeckGateway;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.rewards.ItemType;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ChanceItemRewardIntegrationTest {
  private final MockPlayerStateGateway player = new MockPlayerStateGateway(40, 100, 35);
  private final MockDeckGateway deck = new MockDeckGateway();
  private final FaultyPlayerRunState state = new FaultyPlayerRunState();
  private final PlayerRunStateItemAdapter items = new PlayerRunStateItemAdapter(state);

  private ChanceOutcomeApplier applier() {
    return new ChanceOutcomeApplier(player, new MockCardCatalogGateway("bandage"), deck, items);
  }

  private ChanceEncounterSession session(ChanceOutcome outcome, ChanceOutcomeApplier applier) {
    return new ChanceEncounterSession(
        1,
        new ChanceEncounter(
            "test-item-event", "Test", List.of(new ChanceChoice("accept", "Accept", outcome))),
        applier,
        (node, success) -> {});
  }

  @Test
  void shouldGrantCombinedRewardsAndResolveOnlyOnce() {
    state.addOwnedItem(ItemType.ENERGY_CRYSTAL);
    state.addCalls = 0;
    ChanceOutcome outcome =
        ChanceOutcome.withRewards(
            10, 5, List.of("bandage"), List.of("ENERGY_CRYSTAL", "ENERGY_CRYSTAL"));
    AtomicInteger completed = new AtomicInteger();
    EncounterFlowController flow =
        new EncounterFlowController(
            player,
            new MockCardCatalogGateway("bandage"),
            deck,
            mock(ShopTransactionGateway.class),
            items,
            (node, success) -> completed.incrementAndGet());
    ChanceEncounterSession session =
        flow.startChance(
            1,
            new ChanceEncounter(
                "test-item-event", "Test", List.of(new ChanceChoice("accept", "Accept", outcome))));

    assertEquals(ChanceResolution.Status.APPLIED, session.resolveChoice("accept").getStatus());
    assertEquals(
        ChanceResolution.Status.ALREADY_RESOLVED, session.resolveChoice("accept").getStatus());
    assertEquals(3, state.getOwnedItemCount(ItemType.ENERGY_CRYSTAL));
    assertEquals(List.of("bandage"), deck.getCardIds());
    assertEquals(50, player.getHealth());
    assertEquals(40, player.getCurrency());
    assertTrue(session.complete());
    assertFalse(session.complete());
    assertEquals(1, completed.get());
  }

  @Test
  void shouldValidateAllItemsBeforeAnyCardOrPlayerMutation() {
    ChanceResolution result =
        applier()
            .apply(
                ChanceOutcome.withRewards(
                    10, 5, List.of("bandage"), List.of("ENERGY_CRYSTAL", "UNKNOWN_ITEM")));
    assertEquals(ChanceResolution.Status.ITEM_NOT_FOUND, result.getStatus());
    assertUnchanged();
    assertEquals(0, state.addCalls);
    assertEquals(
        ChanceResolution.Status.INVALID_ITEM_REWARD,
        applier().apply(ChanceOutcome.withItemRewards(0, 0, List.of(" "))).getStatus());
  }

  @Test
  void shouldRejectMissingItemServiceWithoutChangingLegacyRewards() {
    ChanceOutcomeApplier legacy =
        new ChanceOutcomeApplier(player, new MockCardCatalogGateway("bandage"), deck);
    assertEquals(
        ChanceResolution.Status.ITEM_NOT_FOUND,
        legacy.apply(ChanceOutcome.withItemRewards(0, 0, List.of("ENERGY_CRYSTAL"))).getStatus());
    assertUnchanged();
    assertTrue(legacy.apply(new ChanceOutcome(10, 5, "bandage")).isSuccess());
    assertEquals(List.of("bandage"), deck.getCardIds());
    assertEquals(50, player.getHealth());
    assertEquals(40, player.getCurrency());
  }

  @Test
  void shouldRejectUnaffordableAndOverflowingOutcomesBeforeItemAcquisition() {
    assertEquals(
        ChanceResolution.Status.INSUFFICIENT_CURRENCY,
        applier().apply(ChanceOutcome.withItemRewards(0, -36, List.of("LUCKY_COIN"))).getStatus());
    assertEquals(
        ChanceResolution.Status.ARITHMETIC_OVERFLOW,
        applier()
            .apply(ChanceOutcome.withItemRewards(0, Integer.MAX_VALUE, List.of("LUCKY_COIN")))
            .getStatus());
    assertUnchanged();
    assertEquals(0, state.addCalls);
  }

  @Test
  void shouldNotAcquireItemsWhenTheDeckRejectsItsReward() {
    deck.setFailAdd(true);
    assertEquals(
        ChanceResolution.Status.CARD_ADD_FAILED,
        applier()
            .apply(ChanceOutcome.withRewards(10, 5, List.of("bandage"), List.of("LUCKY_COIN")))
            .getStatus());
    assertUnchanged();
    assertEquals(0, state.addCalls);
  }

  @Test
  void shouldRollbackARejectedAdditionAndAllowACompleteRetry() {
    state.rejectNextAddition = true;
    ChanceEncounterSession session =
        session(ChanceOutcome.withItemRewards(0, 0, List.of("LUCKY_COIN")), applier());
    assertEquals(
        ChanceResolution.Status.ITEM_ADD_FAILED, session.resolveChoice("accept").getStatus());
    assertUnchanged();
    assertTrue(session.resolveChoice("accept").isSuccess());
    assertEquals(List.of(ItemType.LUCKY_COIN), state.getOwnedItems());
  }

  @Test
  void shouldRollbackEarlierRewardsWhenSecondItemThrowsBeforeMutation() {
    state.throwAt = 2;
    assertEquals(
        ChanceResolution.Status.ITEM_ADD_FAILED,
        applier()
            .apply(
                ChanceOutcome.withRewards(
                    10, 5, List.of("bandage"), List.of("ENERGY_CRYSTAL", "LUCKY_COIN")))
            .getStatus());
    assertUnchanged();
  }

  @Test
  void shouldUndoAnAdditionThatThrowsAfterMutationAndRestoreOldOrder() {
    List<ItemType> old = List.of(ItemType.ENERGY_CRYSTAL, ItemType.MERCHANTS_FAVOR);
    state.restoreOwnedItems(old);
    state.throwAt = 2;
    state.throwAfterMutation = true;
    assertEquals(
        ChanceResolution.Status.ITEM_ADD_FAILED,
        applier()
            .apply(
                ChanceOutcome.withRewards(
                    10, 5, List.of("bandage"), List.of("ENERGY_CRYSTAL", "LUCKY_COIN")))
            .getStatus());
    assertEquals(old, state.getOwnedItems());
    assertTrue(deck.getCardIds().isEmpty());
    assertEquals(40, player.getHealth());
    assertEquals(35, player.getCurrency());
  }

  @Test
  void shouldRestoreItemsAndCardsWhenCurrencyFails() {
    player.failNextCurrencyUpdate();
    assertEquals(
        ChanceResolution.Status.PLAYER_UPDATE_FAILED,
        applier()
            .apply(ChanceOutcome.withRewards(10, 5, List.of("bandage"), List.of("ENERGY_CRYSTAL")))
            .getStatus());
    assertUnchanged();
  }

  @Test
  void shouldRestoreItemsCurrencyAndCardsWhenHealthRejectsMutation() {
    player.rejectNextHealthUpdate();
    assertEquals(
        ChanceResolution.Status.PLAYER_UPDATE_FAILED,
        applier()
            .apply(ChanceOutcome.withRewards(10, 5, List.of("bandage"), List.of("ENERGY_CRYSTAL")))
            .getStatus());
    assertUnchanged();
  }

  @Test
  void shouldRestoreItemsWhenCardCommitFailsBeforeHealthChanges() {
    DeckGateway failingDeck = spy(deck);
    doThrow(new IllegalStateException("commit failed"))
        .when(failingDeck)
        .commitCardAddition("bandage");
    ChanceOutcomeApplier applier =
        new ChanceOutcomeApplier(player, new MockCardCatalogGateway("bandage"), failingDeck, items);
    assertEquals(
        ChanceResolution.Status.CARD_ADD_FAILED,
        applier
            .apply(ChanceOutcome.withRewards(0, 5, List.of("bandage"), List.of("ENERGY_CRYSTAL")))
            .getStatus());
    assertUnchanged();
  }

  @Test
  void shouldBlockRetryWhenItemRollbackFails() {
    state.failRemoval = true;
    player.failNextCurrencyUpdate();
    ChanceEncounterSession session =
        session(ChanceOutcome.withItemRewards(0, 5, List.of("ENERGY_CRYSTAL")), applier());
    ChanceResolution result = session.resolveChoice("accept");
    assertEquals(ChanceResolution.Status.ROLLBACK_FAILED, result.getStatus());
    assertSame(result, session.resolveChoice("accept"));
    assertEquals(1, state.addCalls);
    assertEquals(List.of(ItemType.ENERGY_CRYSTAL), state.getOwnedItems());
    assertFalse(session.complete());
  }

  @Test
  void shouldKeepOwnershipAndBlockRetryAfterIrreversibleHealthFailure() {
    PlayerStateGateway throwingPlayer = spy(player);
    doAnswer(
            invocation -> {
              invocation.callRealMethod();
              throw new IllegalStateException("listener failed after health mutation");
            })
        .when(throwingPlayer)
        .applyDirectHealthChange(anyInt());
    ChanceOutcomeApplier applier =
        new ChanceOutcomeApplier(
            throwingPlayer, new MockCardCatalogGateway("bandage"), deck, items);
    ChanceEncounterSession session =
        session(
            ChanceOutcome.withRewards(-50, 5, List.of("bandage"), List.of("ENERGY_CRYSTAL")),
            applier);
    assertEquals(
        ChanceResolution.Status.ROLLBACK_FAILED, session.resolveChoice("accept").getStatus());
    assertEquals(
        ChanceResolution.Status.ROLLBACK_FAILED, session.resolveChoice("accept").getStatus());
    assertEquals(0, throwingPlayer.getHealth());
    assertEquals(List.of("bandage"), deck.getCardIds());
    assertEquals(1, state.addCalls);
    assertEquals(List.of(ItemType.ENERGY_CRYSTAL), state.getOwnedItems());
  }

  @Test
  void shouldRejectReentrantSettlementFromAPlayerListener() {
    PlayerStateGateway reentrantPlayer = spy(player);
    AtomicReference<ChanceEncounterSession> reference = new AtomicReference<>();
    AtomicReference<ChanceResolution> nested = new AtomicReference<>();
    doAnswer(
            invocation -> {
              invocation.callRealMethod();
              nested.set(reference.get().resolveChoice("accept"));
              return null;
            })
        .when(reentrantPlayer)
        .setCurrency(anyInt());
    reference.set(
        session(
            ChanceOutcome.withItemRewards(0, 5, List.of("ENERGY_CRYSTAL")),
            new ChanceOutcomeApplier(
                reentrantPlayer, new MockCardCatalogGateway("bandage"), deck, items)));
    assertTrue(reference.get().resolveChoice("accept").isSuccess());
    assertEquals(ChanceResolution.Status.RESOLUTION_IN_PROGRESS, nested.get().getStatus());
    assertEquals(1, state.addCalls);
  }

  @Test
  void shouldReportCatalogueExceptionsBeforeMutation() {
    ItemRewardGateway broken = spy(items);
    doThrow(new IllegalStateException("catalogue failed")).when(broken).containsItem("LUCKY_COIN");
    ChanceResolution result =
        new ChanceOutcomeApplier(player, new MockCardCatalogGateway("bandage"), deck, broken)
            .apply(ChanceOutcome.withItemRewards(0, 0, List.of("LUCKY_COIN")));
    assertEquals(ChanceResolution.Status.ITEM_NOT_FOUND, result.getStatus());
    assertUnchanged();
  }

  private void assertUnchanged() {
    assertTrue(state.getOwnedItems().isEmpty());
    assertTrue(deck.getCardIds().isEmpty());
    assertEquals(40, player.getHealth());
    assertEquals(35, player.getCurrency());
  }

  private static final class FaultyPlayerRunState extends PlayerRunState {
    private int addCalls;
    private int throwAt;
    private boolean throwAfterMutation;
    private boolean rejectNextAddition;
    private boolean failRemoval;

    private FaultyPlayerRunState() {
      super(40, 100, 35);
    }

    @Override
    public void addOwnedItem(ItemType item) {
      addCalls++;
      if (rejectNextAddition) {
        rejectNextAddition = false;
        return;
      }
      if (addCalls == throwAt && !throwAfterMutation) {
        throw new IllegalStateException("addition failed before mutation");
      }
      super.addOwnedItem(item);
      if (addCalls == throwAt) {
        throw new IllegalStateException("addition failed after mutation");
      }
    }

    @Override
    public boolean removeOwnedItem(ItemType item) {
      return !failRemoval && super.removeOwnedItem(item);
    }
  }
}

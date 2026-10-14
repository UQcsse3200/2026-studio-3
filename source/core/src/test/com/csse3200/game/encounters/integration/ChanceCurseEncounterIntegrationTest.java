package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.chance.ChanceEncounter;
import com.csse3200.game.chance.ChanceEncounterConfigLoader;
import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.encounters.integration.mocks.MockCardCatalogGateway;
import com.csse3200.game.encounters.integration.mocks.MockDeckGateway;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.rewards.ItemType;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests configured transactions against the real ownership adapter. Without the unmerged Team 7
 * catalogue, curses must fail safely; dependency validation sets chance.requireCurseItems=true.
 */
@ExtendWith(GameExtension.class)
class ChanceCurseEncounterIntegrationTest {
  private final MockPlayerStateGateway player = new MockPlayerStateGateway(40, 100, 35);
  private final PlayerRunState state = new PlayerRunState(40, 100, 35);
  private final MockDeckGateway deck = new MockDeckGateway();
  private final PlayerRunStateItemAdapter items = new PlayerRunStateItemAdapter(state);

  private ChanceEncounter encounter(String id) {
    return ChanceEncounterConfigLoader.loadEncounters().stream()
        .filter(event -> event.getId().equals(id))
        .findFirst()
        .orElseThrow();
  }

  private ChanceOutcomeApplier applier() {
    return new ChanceOutcomeApplier(player, new MockCardCatalogGateway("bandage"), deck, items);
  }

  private boolean available(String id) {
    boolean available = items.containsItem(id);
    if (Boolean.getBoolean("chance.requireCurseItems")) {
      assertTrue(available, "Dependency validation requires Team 7 item " + id);
    }
    return available;
  }

  @Test
  void shouldGrantBrand() {
    shouldGrantConfiguredCurseOnceOrRejectMissingDependency(
        "mysterious-shrine", "accept-brand", "BRAND_OF_THE_DAMNED", 50);
  }

  @Test
  void shouldGrantBloodOath() {
    shouldGrantConfiguredCurseOnceOrRejectMissingDependency(
        "mysterious-shrine", "swear-blood-oath", "BLOOD_OATH_SIGIL", 70);
  }

  @Test
  void shouldGrantShatteredAegis() {
    shouldGrantConfiguredCurseOnceOrRejectMissingDependency(
        "abandoned-mine", "claim-shattered-aegis", "SHATTERED_AEGIS", 50);
  }

  @Test
  void shouldGrantAshenEmber() {
    shouldGrantConfiguredCurseOnceOrRejectMissingDependency(
        "abandoned-mine", "take-ashen-ember", "ASHEN_EMBER", 80);
  }

  private void shouldGrantConfiguredCurseOnceOrRejectMissingDependency(
      String eventId, String choiceId, String itemId, int gold) {
    ChanceEncounter event = encounter(eventId);
    ChanceOutcome outcome = event.resolveChoice(choiceId);
    assertEquals(4, event.getChoices().size());
    assertEquals(0, outcome.getHealthDelta());
    assertEquals(gold, outcome.getCurrencyDelta());
    assertEquals(List.of(itemId), outcome.getItemRewardIds());
    assertTrue(outcome.getCardRewardIds().isEmpty());
    AtomicInteger completions = new AtomicInteger();
    ChanceEncounterSession session =
        new ChanceEncounterSession(
            1, event, applier(), (node, success) -> completions.incrementAndGet());
    boolean available = available(itemId);
    ChanceResolution result = session.resolveChoice(choiceId);
    if (!available) {
      assertEquals(ChanceResolution.Status.ITEM_NOT_FOUND, result.getStatus());
      assertEquals(35, player.getCurrency());
      assertTrue(state.getOwnedItems().isEmpty());
      assertFalse(session.complete());
    } else {
      assertTrue(result.isSuccess());
      assertEquals(List.of(ItemType.valueOf(itemId)), state.getOwnedItems());
      assertEquals(35 + gold, player.getCurrency());
      assertEquals(
          ChanceResolution.Status.ALREADY_RESOLVED, session.resolveChoice(choiceId).getStatus());
      assertEquals(
          ChanceResolution.Status.ALREADY_RESOLVED, session.resolveChoice("leave").getStatus());
      assertTrue(session.complete());
      assertFalse(session.complete());
      assertEquals(1, completions.get());
    }
    assertEquals(40, player.getHealth());
    assertEquals(40, state.getCurrentHealth());
    assertEquals(100, state.getMaxHealth());
    assertTrue(deck.getCardIds().isEmpty());
  }

  @Test
  void shouldAllowAnotherCopyOfBrand() {
    shouldAllowAnotherCopyAcrossSessionsSharingDurableState(
        "mysterious-shrine", "accept-brand", "BRAND_OF_THE_DAMNED", 50);
  }

  @Test
  void shouldAllowAnotherCopyOfBloodOath() {
    shouldAllowAnotherCopyAcrossSessionsSharingDurableState(
        "mysterious-shrine", "swear-blood-oath", "BLOOD_OATH_SIGIL", 70);
  }

  @Test
  void shouldAllowAnotherCopyOfShatteredAegis() {
    shouldAllowAnotherCopyAcrossSessionsSharingDurableState(
        "abandoned-mine", "claim-shattered-aegis", "SHATTERED_AEGIS", 50);
  }

  @Test
  void shouldAllowAnotherCopyOfAshenEmber() {
    shouldAllowAnotherCopyAcrossSessionsSharingDurableState(
        "abandoned-mine", "take-ashen-ember", "ASHEN_EMBER", 80);
  }

  private void shouldAllowAnotherCopyAcrossSessionsSharingDurableState(
      String eventId, String choiceId, String itemId, int gold) {
    ChanceEncounter event = encounter(eventId);
    if (!available(itemId)) {
      assertEquals(
          ChanceResolution.Status.ITEM_NOT_FOUND,
          applier().apply(event.resolveChoice(choiceId)).getStatus());
      return;
    }
    for (int node = 1; node <= 2; node++) {
      ChanceEncounterSession session = new ChanceEncounterSession(node, event, applier(), null);
      assertEquals(4, session.getAvailableChoices().size());
      assertTrue(session.resolveChoice(choiceId).isSuccess());
      assertTrue(session.complete());
    }
    assertEquals(2, state.getOwnedItemCount(ItemType.valueOf(itemId)));
    assertEquals(35 + 2 * gold, player.getCurrency());
    assertEquals(40, player.getHealth());
  }

  @Test
  void shouldRollbackOnlyNewCopyOfBrand() {
    shouldRollbackOnlyNewCurseCopyWhenGoldUpdateFails(
        "mysterious-shrine", "accept-brand", "BRAND_OF_THE_DAMNED", 50);
  }

  @Test
  void shouldRollbackOnlyNewCopyOfBloodOath() {
    shouldRollbackOnlyNewCurseCopyWhenGoldUpdateFails(
        "mysterious-shrine", "swear-blood-oath", "BLOOD_OATH_SIGIL", 70);
  }

  @Test
  void shouldRollbackOnlyNewCopyOfShatteredAegis() {
    shouldRollbackOnlyNewCurseCopyWhenGoldUpdateFails(
        "abandoned-mine", "claim-shattered-aegis", "SHATTERED_AEGIS", 50);
  }

  @Test
  void shouldRollbackOnlyNewCopyOfAshenEmber() {
    shouldRollbackOnlyNewCurseCopyWhenGoldUpdateFails(
        "abandoned-mine", "take-ashen-ember", "ASHEN_EMBER", 80);
  }

  private void shouldRollbackOnlyNewCurseCopyWhenGoldUpdateFails(
      String eventId, String choiceId, String itemId, int gold) {
    ChanceEncounter event = encounter(eventId);
    if (!available(itemId)) {
      assertEquals(
          ChanceResolution.Status.ITEM_NOT_FOUND,
          applier().apply(event.resolveChoice(choiceId)).getStatus());
      return;
    }
    state.addOwnedItem(ItemType.valueOf(itemId));
    state.addOwnedItem(ItemType.ENERGY_CRYSTAL);
    List<ItemType> before = state.getOwnedItems();
    player.failNextCurrencyUpdate();
    ChanceEncounterSession session = new ChanceEncounterSession(1, event, applier(), null);
    assertEquals(
        ChanceResolution.Status.PLAYER_UPDATE_FAILED, session.resolveChoice(choiceId).getStatus());
    assertEquals(before, state.getOwnedItems());
    assertEquals(35, player.getCurrency());
    assertEquals(40, player.getHealth());
    assertFalse(session.isResolved());
    assertTrue(session.resolveChoice(choiceId).isSuccess());
    assertEquals(2, state.getOwnedItemCount(ItemType.valueOf(itemId)));
    assertEquals(35 + gold, player.getCurrency());
  }

  @Test
  void shouldApplyCombinedJsonRewardsAndRestoreEverythingOnFailure() {
    ChanceEncounter event =
        ChanceEncounterConfigLoader.loadEncounters("test/chance/combined_item_rewards.json")
            .getFirst();
    state.addOwnedItem(ItemType.ENERGY_CRYSTAL);
    List<ItemType> before = state.getOwnedItems();
    player.rejectNextHealthUpdate();
    ChanceEncounterSession session = new ChanceEncounterSession(1, event, applier(), null);
    assertEquals(
        ChanceResolution.Status.PLAYER_UPDATE_FAILED, session.resolveChoice("accept").getStatus());
    assertEquals(before, state.getOwnedItems());
    assertTrue(deck.getCardIds().isEmpty());
    assertEquals(35, player.getCurrency());
    assertEquals(40, player.getHealth());
    assertTrue(session.resolveChoice("accept").isSuccess());
    assertEquals(3, state.getOwnedItemCount(ItemType.ENERGY_CRYSTAL));
    assertEquals(List.of("bandage"), deck.getCardIds());
    assertEquals(40, player.getCurrency());
    assertEquals(50, player.getHealth());
  }

  @Test
  void shouldRejectUnknownConfiguredItemBeforeOtherRewards() {
    ChanceEncounter event =
        ChanceEncounterConfigLoader.loadEncounters("test/chance/combined_item_rewards.json")
            .getFirst();
    assertEquals(
        ChanceResolution.Status.ITEM_NOT_FOUND,
        applier().apply(event.resolveChoice("unknown")).getStatus());
    assertTrue(state.getOwnedItems().isEmpty());
    assertTrue(deck.getCardIds().isEmpty());
    assertEquals(35, player.getCurrency());
    assertEquals(40, player.getHealth());
  }

  @Test
  void shouldPreserveOriginalTradesAndLeaveChoices() {
    assertTrue(
        applier().apply(encounter("mysterious-shrine").resolveChoice("make-offering")).isSuccess());
    assertEquals(30, player.getHealth());
    assertEquals(60, player.getCurrency());
    assertTrue(
        applier().apply(encounter("abandoned-mine").resolveChoice("search-tunnels")).isSuccess());
    assertEquals(18, player.getHealth());
    assertEquals(90, player.getCurrency());
    for (String id : List.of("mysterious-shrine", "abandoned-mine")) {
      assertTrue(applier().apply(encounter(id).resolveChoice("leave")).isSuccess());
    }
    assertEquals(18, player.getHealth());
    assertEquals(90, player.getCurrency());
    assertTrue(state.getOwnedItems().isEmpty());
  }
}

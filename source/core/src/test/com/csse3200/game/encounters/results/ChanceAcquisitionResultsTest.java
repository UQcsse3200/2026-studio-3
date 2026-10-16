package com.csse3200.game.encounters.results;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.TestCardService;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.chance.ChanceChoice;
import com.csse3200.game.chance.ChanceEncounter;
import com.csse3200.game.chance.ChanceEncounterBehaviourFactory;
import com.csse3200.game.chance.ChanceEncounterConfigLoader;
import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.chance.TestMineRandom;
import com.csse3200.game.encounters.integration.*;
import com.csse3200.game.encounters.integration.mocks.MockCardCatalogGateway;
import com.csse3200.game.encounters.integration.mocks.MockDeckGateway;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.PlayerRunState;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ChanceAcquisitionResultsTest {
  private final MockPlayerStateGateway player = new MockPlayerStateGateway(45, 50, 35);
  private final MockDeckGateway deck = new MockDeckGateway();
  private final PlayerRunState state = new PlayerRunState(45, 50, 35);

  private ChanceEncounterSession session(ChanceOutcome outcome) {
    return new ChanceEncounterSession(
        1,
        new ChanceEncounter("test", "Test", List.of(new ChanceChoice("accept", "Accept", outcome))),
        new ChanceOutcomeApplier(
            player,
            new MockCardCatalogGateway("bandage", "strike"),
            deck,
            new PlayerRunStateItemAdapter(state)),
        null);
  }

  private AcquisitionResult.Entry entry(
      AcquisitionResult result, AcquisitionResult.Kind kind, String id) {
    return result.entries().stream()
        .filter(e -> e.kind() == kind && e.id().equals(id))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void shouldMapBrandTrade() {
    assertCurse(
        "mysterious-shrine",
        "accept-brand",
        "BRAND_OF_THE_DAMNED",
        "Brand of the Damned",
        50,
        "Strength gain");
  }

  @Test
  void shouldMapBloodOathTrade() {
    assertCurse(
        "mysterious-shrine",
        "swear-blood-oath",
        "BLOOD_OATH_SIGIL",
        "Blood-Oath Sigil",
        70,
        "bypassing Block and Armour");
  }

  @Test
  void shouldMapShatteredAegisTrade() {
    assertCurse(
        "abandoned-mine", "search-tunnels", "SHATTERED_AEGIS", "Shattered Aegis", 60, "Block gain");
  }

  @Test
  void shouldMapAshenEmberTrade() {
    assertCurse(
        "abandoned-mine",
        "search-tunnels",
        "ASHEN_EMBER",
        "Ashen Ember",
        60,
        "player is recreated");
  }

  private void assertCurse(
      String eventId, String choiceId, String itemId, String name, int gold, String effect) {
    ChanceEncounter event =
        ChanceEncounterConfigLoader.loadEncounters().stream()
            .filter(e -> e.getId().equals(eventId))
            .findFirst()
            .orElseThrow();
    // Test-only ownership double permits configuration validation without importing Team 7 history.
    ItemRewardGateway items = mock(ItemRewardGateway.class);
    when(items.containsItem(itemId)).thenReturn(true);
    when(items.addItem(itemId)).thenReturn(true);
    ChanceEncounterSession session =
        new ChanceEncounterSession(
            1,
            event,
            ChanceEncounterBehaviourFactory.create(
                event,
                new TestMineRandom(
                    0,
                    List.of(
                            "BRAND_OF_THE_DAMNED",
                            "BLOOD_OATH_SIGIL",
                            "SHATTERED_AEGIS",
                            "ASHEN_EMBER")
                        .indexOf(itemId)),
                TestCardService.withCards("bandage")),
            new ChanceOutcomeApplier(player, new MockCardCatalogGateway(), deck, items),
            null);
    assertTrue(session.resolveChoice(choiceId).isSuccess());
    AcquisitionResult result = session.getAcquisitionResult(null).orElseThrow();
    assertEquals(2, result.entries().size());
    assertEquals(gold, entry(result, AcquisitionResult.Kind.GOLD, "gold").delta());
    AcquisitionResult.Entry item = entry(result, AcquisitionResult.Kind.ITEM, itemId);
    assertEquals(name, item.name());
    assertEquals(1, item.quantity());
    assertNull(item.durationTurns());
    assertTrue(item.description().contains(effect));
    assertTrue(item.description().contains("per copy"));
    assertTrue(item.description().contains("minimum"));
    assertEquals(45, player.getHealth());
    assertSame(result, session.getAcquisitionResult(null).orElseThrow());
    verify(items, times(1)).addItem(itemId);
  }

  @Test
  void shouldMapActualCappedHealingAndGoldLoss() {
    ChanceEncounterSession session = session(new ChanceOutcome(20, -15));
    assertTrue(session.resolveChoice("accept").isSuccess());
    AcquisitionResult result = session.getAcquisitionResult(null).orElseThrow();
    assertEquals(5, entry(result, AcquisitionResult.Kind.HP, "hp").delta());
    assertEquals(-15, entry(result, AcquisitionResult.Kind.GOLD, "gold").delta());
    assertEquals(2, result.entries().size());
  }

  @Test
  void shouldMapDamageAndGoldGain() {
    ChanceEncounterSession session = session(new ChanceOutcome(-8, 25));
    assertTrue(session.resolveChoice("accept").isSuccess());
    AcquisitionResult result = session.getAcquisitionResult(null).orElseThrow();
    assertEquals(-8, entry(result, AcquisitionResult.Kind.HP, "hp").delta());
    assertEquals(25, entry(result, AcquisitionResult.Kind.GOLD, "gold").delta());
  }

  @Test
  void shouldCombineDuplicateGoodsUsingRealNamesWithoutExtraEffects() {
    CardService cards = mock(CardService.class);
    CardConfig bandage = new CardConfig();
    bandage.name = "Bandage";
    bandage.description = "Restore health.";
    when(cards.getCard("bandage")).thenReturn(Optional.of(bandage));
    when(cards.getCard("strike")).thenReturn(Optional.empty());
    ChanceEncounterSession session =
        session(
            ChanceOutcome.withRewards(
                1,
                5,
                List.of("bandage", "bandage", "strike"),
                List.of("LUCKY_COIN", "LUCKY_COIN", "ENERGY_CRYSTAL")));
    assertTrue(session.resolveChoice("accept").isSuccess());
    AcquisitionResult result = session.getAcquisitionResult(cards).orElseThrow();
    assertEquals(6, result.entries().size());
    assertEquals(2, entry(result, AcquisitionResult.Kind.CARD, "bandage").quantity());
    assertEquals("Bandage", entry(result, AcquisitionResult.Kind.CARD, "bandage").name());
    assertEquals(
        "Restore health.", entry(result, AcquisitionResult.Kind.CARD, "bandage").description());
    assertEquals(1, entry(result, AcquisitionResult.Kind.CARD, "strike").quantity());
    assertEquals(2, entry(result, AcquisitionResult.Kind.ITEM, "LUCKY_COIN").quantity());
    assertEquals(
        "Shard of Fortune", entry(result, AcquisitionResult.Kind.ITEM, "LUCKY_COIN").name());
    assertEquals(3, state.getOwnedItems().size());
    assertEquals(3, deck.getCardIds().size());
    assertFalse(
        result.entries().stream()
            .anyMatch(e -> e.kind() == AcquisitionResult.Kind.NEGATIVE_EFFECT));
  }

  @Test
  void shouldOmitZeroChangesAndNoEffectLeave() {
    player.setHealth(50);
    ChanceEncounterSession capped = session(new ChanceOutcome(20, 0));
    assertTrue(capped.resolveChoice("accept").isSuccess());
    assertTrue(capped.getAcquisitionResult(null).isEmpty());
    ChanceEncounterSession leave = session(new ChanceOutcome(0, 0));
    assertTrue(leave.resolveChoice("accept").isSuccess());
    assertTrue(leave.getAcquisitionResult(null).isEmpty());
    assertTrue(leave.complete());
  }

  @Test
  void shouldHaveNoResultBeforeCommitOrAfterFailedAndPartialSettlement() {
    ChanceEncounterSession session =
        session(ChanceOutcome.withRewards(1, 5, List.of("bandage"), List.of("LUCKY_COIN")));
    assertTrue(session.getAcquisitionResult(null).isEmpty());
    player.failNextCurrencyUpdate();
    ChanceResolution failed = session.resolveChoice("accept");
    assertFalse(failed.isSuccess());
    assertTrue(session.getAcquisitionResult(null).isEmpty());
    assertTrue(ChanceAcquisitionResults.fromApplied(failed, "failed", null).isEmpty());
    assertTrue(state.getOwnedItems().isEmpty());
    assertTrue(deck.getCardIds().isEmpty());
    player.failAllCurrencyUpdates();
    ChanceResolution partial = session.resolveChoice("accept");
    assertEquals(ChanceResolution.Status.ROLLBACK_FAILED, partial.getStatus());
    assertTrue(session.getAcquisitionResult(null).isEmpty());
    assertTrue(ChanceAcquisitionResults.fromApplied(partial, "partial", null).isEmpty());
  }

  @Test
  void shouldRetainSnapshotAcrossRepeatedQueriesAndCompletion() {
    ChanceEncounterSession session = session(new ChanceOutcome(1, 5, "bandage"));
    assertTrue(session.resolveChoice("accept").isSuccess());
    AcquisitionResult result = session.getAcquisitionResult(null).orElseThrow();
    player.setCurrency(0);
    assertSame(result, session.getAcquisitionResult(mock(CardService.class)).orElseThrow());
    assertEquals(5, entry(result, AcquisitionResult.Kind.GOLD, "gold").delta());
    assertEquals(
        ChanceResolution.Status.ALREADY_RESOLVED, session.resolveChoice("accept").getStatus());
    assertTrue(session.complete());
    assertSame(result, session.getAcquisitionResult(null).orElseThrow());
    assertThrows(UnsupportedOperationException.class, result.entries()::clear);
    ChanceEncounterSession next = session(new ChanceOutcome(0, 5));
    assertTrue(next.resolveChoice("accept").isSuccess());
    assertNotEquals(result.resultId(), next.getAcquisitionResult(null).orElseThrow().resultId());
  }
}

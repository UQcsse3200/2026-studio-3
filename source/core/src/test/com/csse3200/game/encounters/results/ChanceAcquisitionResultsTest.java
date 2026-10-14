package com.csse3200.game.encounters.results;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.encounters.integration.ChanceOutcomeApplier;
import com.csse3200.game.encounters.integration.PlayerRunStateItemAdapter;
import com.csse3200.game.encounters.integration.mocks.MockCardCatalogGateway;
import com.csse3200.game.encounters.integration.mocks.MockDeckGateway;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import com.csse3200.game.maps.PlayerRunState;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChanceAcquisitionResultsTest {
  @Test
  void displaysCappedHealingAndCommittedGoldChangeRatherThanRequestedValues() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(95, 100, 30);
    var resolution = new ChanceOutcomeApplier(player).apply(new ChanceOutcome(20, -10));

    var display = ChanceAcquisitionResults.fromApplied(resolution, "event:1", null).orElseThrow();
    assertEquals(2, display.entries().size());
    assertEquals(5, display.entries().get(0).quantity());
    assertEquals(AcquisitionResult.Kind.POSITIVE_EFFECT, display.entries().get(0).kind());
    assertEquals(10, display.entries().get(1).quantity());
    assertEquals(AcquisitionResult.Kind.NEGATIVE_EFFECT, display.entries().get(1).kind());
    assertEquals(20, player.getCurrency());
  }

  @Test
  void ignoresUnappliedOrEmptyOutcomes() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(50, 0);
    var failed = new ChanceOutcomeApplier(player).apply(new ChanceOutcome(0, -10));
    assertTrue(ChanceAcquisitionResults.fromApplied(failed, "failed", null).isEmpty());

    var unchanged = new ChanceOutcomeApplier(player).apply(new ChanceOutcome(0, 0));
    assertTrue(ChanceAcquisitionResults.fromApplied(unchanged, "empty", null).isEmpty());
  }

  @Test
  void showsOnlyCommittedItemsAndTheirConfiguredEffectDescription() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(50, 100, 30);
    PlayerRunState state = new PlayerRunState(50, 100, 30);
    var applier =
        new ChanceOutcomeApplier(
            player,
            new MockCardCatalogGateway(),
            new MockDeckGateway(),
            new PlayerRunStateItemAdapter(state));
    var applied = applier.apply(ChanceOutcome.withItemRewards(0, 5, List.of("ENERGY_CRYSTAL")));

    var result =
        ChanceAcquisitionResults.fromApplied(
                applied, "event:item", null, "The item's effect applies in future battles.")
            .orElseThrow();
    assertEquals(2, result.entries().size());
    assertEquals(AcquisitionResult.Kind.ITEM, result.entries().get(1).kind());
    assertEquals("Ember of the Divine", result.entries().get(1).name());
    assertEquals(
        "The item's effect applies in future battles.", result.entries().get(1).description());
    assertEquals(1, state.getOwnedItems().size());

    var rejected = applier.apply(ChanceOutcome.withItemRewards(0, 0, List.of("UNKNOWN_ITEM")));
    assertTrue(ChanceAcquisitionResults.fromApplied(rejected, "event:rejected", null).isEmpty());
  }
}

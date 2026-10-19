package com.csse3200.game.chance;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.extensions.GameExtension;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(GameExtension.class)
class ChanceMineRiskConfigTest {
  @TempDir Path directory;

  private ChanceEncounter load(String risk) throws Exception {
    String config =
        """
        {"encounters":[{"id":"abandoned-mine","description":"Mine","weight":1,"choices":[
          {"id":"search-tunnels","description":"Explore","outcome":{"healthDelta":0,"currencyDelta":60},"curseRisk":%s},
          {"id":"leave","description":"Leave","outcome":{"healthDelta":0,"currencyDelta":0}}
        ]}]}
        """
            .formatted(risk);
    Path file = directory.resolve("risk.json");
    Files.writeString(file, config);
    return ChanceEncounterConfigLoader.loadEncounters(file.toString()).getFirst();
  }

  @Test
  void shouldParseTheProductionPoolAsImmutableWithEqualWeights() {
    ChanceEncounter mine = ChanceEncounterConfigLoader.loadEncounters().get(3);
    assertEquals(
        List.of("search-tunnels", "leave"),
        mine.getChoices().stream().map(ChanceChoice::getId).toList());
    ChanceCurseRisk risk = mine.getChoices().getFirst().getCurseRisk().orElseThrow();
    assertEquals(0.5, risk.triggerChance());
    assertEquals(
        List.of("BRAND_OF_THE_DAMNED", "BLOOD_OATH_SIGIL", "SHATTERED_AEGIS", "ASHEN_EMBER"),
        risk.items().stream().map(ChanceCurseRisk.WeightedItem::id).toList());
    assertEquals(
        List.of(1, 1, 1, 1),
        risk.items().stream().map(ChanceCurseRisk.WeightedItem::weight).toList());
    assertThrows(UnsupportedOperationException.class, risk.items()::clear);
    assertEquals(60, mine.resolveChoice("search-tunnels").getCurrencyDelta());
    assertNull(mine.resolveChoice("claim-shattered-aegis"));
    assertNull(mine.resolveChoice("take-ashen-ember"));
  }

  @Test
  void shouldRejectInvalidProbabilityAndPoolShapes() {
    for (String risk :
        List.of(
            "null",
            "[]",
            "{}",
            "{\"triggerChance\":\"0.5\",\"items\":[{\"id\":\"LUCKY_COIN\",\"weight\":1}]}",
            "{\"triggerChance\":-0.1,\"items\":[{\"id\":\"LUCKY_COIN\",\"weight\":1}]}",
            "{\"triggerChance\":1.1,\"items\":[{\"id\":\"LUCKY_COIN\",\"weight\":1}]}",
            "{\"triggerChance\":1e309,\"items\":[{\"id\":\"LUCKY_COIN\",\"weight\":1}]}",
            "{\"triggerChance\":0.5,\"items\":[]}",
            "{\"triggerChance\":0.5,\"items\":[null]}",
            "{\"triggerChance\":0.5,\"items\":[{\"id\":3,\"weight\":1}]}",
            "{\"triggerChance\":0.5,\"items\":[{\"id\":\"LUCKY_COIN\",\"weight\":1.5}]}",
            "{\"triggerChance\":0.5,\"items\":[{\"id\":\"LUCKY_COIN\",\"weight\":\"1\"}]}")) {
      assertThrows(ChanceEncounterLoadingException.class, () -> load(risk), risk);
    }
  }

  @Test
  void shouldRejectMissingInvalidDuplicateOrOverflowingWeightsAndIds() {
    for (String items :
        List.of(
            "[{\"id\":\"LUCKY_COIN\"}]",
            "[{\"weight\":1}]",
            "[{\"id\":\"ashen-ember\",\"weight\":1}]",
            "[{\"id\":\"LUCKY_COIN\",\"weight\":0}]",
            "[{\"id\":\"LUCKY_COIN\",\"weight\":-1}]",
            "[{\"id\":\"LUCKY_COIN\",\"weight\":1},{\"id\":\"LUCKY_COIN\",\"weight\":2}]",
            "[{\"id\":\"LUCKY_COIN\",\"weight\":2147483647},{\"id\":\"ENERGY_CRYSTAL\",\"weight\":1}]")) {
      assertThrows(
          ChanceEncounterLoadingException.class,
          () -> load("{\"triggerChance\":0.5,\"items\":" + items + "}"));
    }
  }

  @Test
  void shouldAcceptProbabilityEndpointsAndUnequalWeights() throws Exception {
    for (int chance : List.of(0, 1)) {
      ChanceEncounter event =
          load(
              "{\"triggerChance\":"
                  + chance
                  + ",\"items\":[{\"id\":\"LUCKY_COIN\",\"weight\":1},{\"id\":\"ENERGY_CRYSTAL\",\"weight\":3}]}");
      assertEquals(
          chance, event.getChoices().getFirst().getCurseRisk().orElseThrow().triggerChance());
    }
  }
}

package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.extensions.GameExtension;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(GameExtension.class)
class CardRewardPolicyLoaderTest {
  @TempDir Path directory;

  @Test
  void loadsApprovedProductionWeights() {
    CardRewardPolicy policy = CardRewardPolicyLoader.loadDefault();
    assertEquals(new CardRewardPolicy.RarityWeights(70, 25, 5), policy.normal());
    assertEquals(new CardRewardPolicy.RarityWeights(40, 40, 20), policy.elite());
  }

  @Test
  void missingAndMalformedFilesFailWithTheirFilenameRatherThanUsingFallbackWeights()
      throws Exception {
    assertThrows(IllegalArgumentException.class, () -> CardRewardPolicyLoader.load(null));
    assertThrows(IllegalArgumentException.class, () -> CardRewardPolicyLoader.load(""));
    String missing = directory.resolve("missing.json").toString();
    assertTrue(
        assertThrows(IllegalArgumentException.class, () -> CardRewardPolicyLoader.load(missing))
            .getMessage()
            .contains(missing));
    Path invalid = directory.resolve("bad.json");
    Files.writeString(invalid, "{broken");
    assertTrue(
        assertThrows(
                IllegalArgumentException.class,
                () -> CardRewardPolicyLoader.load(invalid.toString()))
            .getMessage()
            .contains("bad.json"));
  }
}

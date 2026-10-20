package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.cards.CardAcquisitionPoolLoader;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.extensions.GameExtension;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Repeatable evidence for first-choice versus whole-offer rarity frequencies. */
@ExtendWith(GameExtension.class)
class CardRewardSamplingTest {
  @Test
  void sampleTenThousandDistinctOffersPerProfileAndWriteReport() throws Exception {
    CardLibrary cards = new CardLibrary(CardConfigLoader.loadCards());
    var pool = CardAcquisitionPoolLoader.loadDefault(cards);
    CardRewardPolicy policy = CardRewardPolicyLoader.loadDefault();
    StringBuilder report = new StringBuilder("# seed=20261013; offers_per_profile=10000\n");
    report
        .append("# revision=")
        .append(
            System.getProperty(
                "rewardSamplingRevision", "working tree; see checklist verification record"))
        .append('\n');
    report.append("# eligible_ids=").append(String.join(",", pool.eligibleCardIds())).append('\n');
    report.append(
        "profile,rarity,weight,first_pick_count,first_pick_percent,offers_containing_rarity,offer_percent,total_picks\n");
    for (CardRewardProfile profile : CardRewardProfile.values()) {
      EnumMap<Rarity, int[]> counts = new EnumMap<>(Rarity.class);
      for (Rarity rarity : Rarity.values()) {
        counts.put(rarity, new int[3]);
      }
      var generator =
          new CardRewardSelectionGenerator(pool, policy, profile, new Random(20261013L));
      for (int offerIndex = 0; offerIndex < 10000; offerIndex++) {
        var offer = generator.generate().orElseThrow();
        assertEquals(3, new HashSet<>(offer.cardIds()).size());
        counts.get(cards.getCard(offer.cardIds().get(0)).orElseThrow().rarity)[0]++;
        HashSet<Rarity> present = new HashSet<>();
        for (String id : offer.cardIds()) {
          Rarity rarity = cards.getCard(id).orElseThrow().rarity;
          counts.get(rarity)[2]++;
          present.add(rarity);
        }
        present.forEach(rarity -> counts.get(rarity)[1]++);
      }
      var weights = policy.weightsFor(profile);
      double total = weights.common() + weights.uncommon() + weights.rare();
      for (Rarity rarity : Rarity.values()) {
        int[] frequency = counts.get(rarity);
        assertEquals(
            weights.weight(rarity) / total,
            frequency[0] / 10000d,
            0.02,
            profile + " " + rarity + " fixed-seed first-pick distribution");
        report.append(
            String.format(
                Locale.ROOT,
                "%s,%s,%.3f,%d,%.2f,%d,%.2f,%d%n",
                profile,
                rarity,
                weights.weight(rarity),
                frequency[0],
                frequency[0] / 100d,
                frequency[1],
                frequency[1] / 100d,
                frequency[2]));
      }
    }
    Path output = Path.of("../build/reports/card-reward-probabilities.csv");
    Files.createDirectories(output.getParent());
    Files.writeString(output, report);
  }
}

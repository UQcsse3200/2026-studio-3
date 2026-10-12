package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.CardUpgradeConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.cards.runtime.ResolvedCard;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Checks every configured upgrade in the official catalogue against the Team 6 upgrade standard.
 *
 * <p>The standard: an upgrade keeps the card's identity (ID, type, target, rarity and effect
 * order), is named {@code "<base name>+"}, never costs more than the base card, never weakens an
 * effect, improves at least one value, duration or the cost, and has its own description.
 *
 * <p>{@link #PENDING_UPGRADES} lists cards that do not have upgrade data yet. When you add an
 * upgrade for one of your cards, remove its ID from this set in the same commit.
 */
@ExtendWith(GameExtension.class)
class CardUpgradeStandardTest {
  /** Card owners remove their IDs from this set in the same commit that adds their upgrades. */
  private static final Set<String> PENDING_UPGRADES =
      Set.of(
          "warding_sweep",
          "sentinels_rebuke",
          "wardens_judgement",
          "unseal_the_breach",
          "poison_flask",
          "poison_blade",
          "poison_cloud",
          "poison_mark",
          "starfall",
          "rift_lance",
          "astral_ward",
          "resurrection",
          "makeshift_shelter",
          "rubble_barricade",
          "emergency_salve",
          "purify");

  private List<CardConfig> cards;

  @BeforeEach
  void setUp() {
    cards = CardConfigLoader.loadCards();
  }

  @Test
  void everyCardOutsideThePendingListDefinesAnUpgrade() {
    List<String> missing = new ArrayList<>();
    for (CardConfig card : cards) {
      if (!PENDING_UPGRADES.contains(card.id) && card.upgrade == null) {
        missing.add(card.id);
      }
    }
    assertTrue(missing.isEmpty(), "cards without upgrade data: " + missing);
  }

  @Test
  void pendingListOnlyContainsKnownCardsThatStillLackUpgrades() {
    CardLibrary library = new CardLibrary(cards);
    for (String id : PENDING_UPGRADES) {
      CardConfig card =
          library
              .getCard(id)
              .orElseThrow(
                  () ->
                      new AssertionError(
                          "Unknown card " + id + "; remove it from PENDING_UPGRADES"));
      assertNull(
          card.upgrade, id + " now has an upgrade; remove it from PENDING_UPGRADES in this test");
    }
  }

  @Test
  void everyConfiguredUpgradeFollowsTheStandard() {
    for (CardConfig card : cards) {
      if (card.upgrade != null) {
        assertFollowsStandard(card);
      }
    }
  }

  @Test
  void initialCardUpgradesResolveToTheirUpgradedValues() {
    CardLibrary library = new CardLibrary(cards);
    CardResolver resolver = new CardResolver();

    assertAll(
        () -> assertUpgraded(library, resolver, "defend", "Defend+", 1, List.of(effect(8, 0))),
        () ->
            assertUpgraded(
                library,
                resolver,
                "poison_dagger",
                "Poison Dagger+",
                1,
                List.of(effect(6, 0), effect(4, 3))),
        () -> assertUpgraded(library, resolver, "expose", "Expose+", 1, List.of(effect(2, 3))),
        () ->
            assertUpgraded(
                library, resolver, "inner_focus", "Inner Focus+", 2, List.of(effect(3, 0))),
        () -> assertUpgraded(library, resolver, "bandage", "Bandage+", 1, List.of(effect(9, 0))));
  }

  private static void assertFollowsStandard(CardConfig card) {
    CardUpgradeConfig upgrade = card.upgrade;
    String id = card.id;

    assertEquals(card.name + "+", upgrade.name, id + ": upgrade name must be the base name + '+'");
    assertEquals(card.rarity, upgrade.rarity, id + ": upgrade must keep the base rarity");
    assertTrue(upgrade.cost <= card.cost, id + ": upgrade must not cost more than the base card");
    assertNotEquals(
        card.description, upgrade.description, id + ": upgrade needs its own description");
    assertEquals(
        card.effects.length,
        upgrade.effects.length,
        id + ": upgrade must keep the same number of effects");

    boolean improved = upgrade.cost < card.cost;
    for (int i = 0; i < card.effects.length; i++) {
      EffectConfig base = card.effects[i];
      EffectConfig upgraded = upgrade.effects[i];
      String path = id + " effects[" + i + "]";

      assertEquals(base.type, upgraded.type, path + ": upgrade must keep effect type and order");
      assertTrue(upgraded.value >= base.value, path + ": upgrade must not lower the value");
      assertTrue(upgraded.duration >= base.duration, path + ": upgrade must not shorten duration");
      improved |= upgraded.value > base.value || upgraded.duration > base.duration;
    }
    assertTrue(improved, id + ": upgrade must improve a value, a duration or the cost");
  }

  private static void assertUpgraded(
      CardLibrary library,
      CardResolver resolver,
      String cardId,
      String expectedName,
      int expectedCost,
      List<int[]> expectedEffects) {
    CardConfig config = library.getCard(cardId).orElseThrow();
    assertNotNull(config.upgrade, cardId + " should define an upgrade");

    ResolvedCard upgraded =
        resolver.resolve(
            config, new CardInstance("standard-" + cardId, cardId, CardInstance.UPGRADED_LEVEL));

    assertTrue(upgraded.upgraded(), cardId);
    assertEquals(expectedName, upgraded.name(), cardId);
    assertEquals(expectedCost, upgraded.cost(), cardId);
    assertEquals(expectedEffects.size(), upgraded.effects().size(), cardId);
    for (int i = 0; i < expectedEffects.size(); i++) {
      assertEquals(expectedEffects.get(i)[0], upgraded.effects().get(i).value, cardId + " value");
      assertEquals(
          expectedEffects.get(i)[1], upgraded.effects().get(i).duration, cardId + " duration");
    }
  }

  private static int[] effect(int value, int duration) {
    return new int[] {value, duration};
  }
}

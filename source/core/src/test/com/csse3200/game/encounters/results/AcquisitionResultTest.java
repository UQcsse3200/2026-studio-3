package com.csse3200.game.encounters.results;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AcquisitionResultTest {
  @Test
  void resourceChangesPreservePositiveAndNegativeSettledAmounts() {
    for (AcquisitionResult.Kind kind :
        List.of(AcquisitionResult.Kind.GOLD, AcquisitionResult.Kind.HP)) {
      for (int amount : new int[] {50, -10, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
        AcquisitionResult.Entry entry =
            new AcquisitionResult.Entry(
                kind, "resource", amount, "Resource", "Settled change", null);
        assertTrue(entry.isResourceChange());
        assertEquals(amount, entry.delta());
        assertEquals(amount, entry.quantity());
        assertNull(entry.durationTurns());
      }
    }
  }

  @Test
  void resourceFactoriesSupplyStableIdentityWithoutLosingTheSign() {
    AcquisitionResult.Entry gold = AcquisitionResult.Entry.gold(50);
    assertEquals(AcquisitionResult.Kind.GOLD, gold.kind());
    assertEquals("gold", gold.id());
    assertEquals("Gold", gold.name());
    assertEquals(50, gold.delta());
    assertNull(gold.durationTurns());

    AcquisitionResult.Entry hp = AcquisitionResult.Entry.hp(-10);
    assertEquals(AcquisitionResult.Kind.HP, hp.kind());
    assertEquals("hp", hp.id());
    assertEquals("HP", hp.name());
    assertEquals(-10, hp.delta());
    assertNull(hp.durationTurns());
  }

  @Test
  void zeroResourceChangesCannotInventAReward() {
    assertThrows(IllegalArgumentException.class, () -> AcquisitionResult.Entry.gold(0));
    assertThrows(IllegalArgumentException.class, () -> AcquisitionResult.Entry.hp(0));
  }

  @Test
  void instantResourceChangesCannotDeclareAnEffectDuration() {
    for (AcquisitionResult.Kind kind :
        List.of(AcquisitionResult.Kind.GOLD, AcquisitionResult.Kind.HP)) {
      for (int duration : new int[] {-1, 0, 1}) {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AcquisitionResult.Entry(kind, "resource", 10, "Resource", "", duration));
      }
    }
  }

  @Test
  void legacyGoodsKeepTheirPositiveCountsAndSixArgumentConstructor() {
    for (AcquisitionResult.Kind kind :
        List.of(AcquisitionResult.Kind.CARD, AcquisitionResult.Kind.ITEM)) {
      AcquisitionResult.Entry entry =
          new AcquisitionResult.Entry(kind, "goods", 2, "Goods", "Two acquired goods", null);
      assertEquals(2, entry.quantity());
      assertFalse(entry.isResourceChange());
      assertThrows(IllegalStateException.class, entry::delta);
      assertThrows(
          IllegalArgumentException.class,
          () -> new AcquisitionResult.Entry(kind, "goods", 2, "Goods", "", 1));
    }
  }

  @Test
  void legacyGoodsAndEffectsStillRejectNonPositiveQuantities() {
    for (AcquisitionResult.Kind kind :
        List.of(
            AcquisitionResult.Kind.CARD,
            AcquisitionResult.Kind.ITEM,
            AcquisitionResult.Kind.POSITIVE_EFFECT,
            AcquisitionResult.Kind.NEGATIVE_EFFECT)) {
      for (int quantity : new int[] {0, -1}) {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AcquisitionResult.Entry(kind, "entry", quantity, "Entry", "", null));
      }
    }
  }

  @Test
  void effectKindsKeepAbsoluteMagnitudesAndOptionalPositiveDurations() {
    for (AcquisitionResult.Kind kind :
        List.of(AcquisitionResult.Kind.POSITIVE_EFFECT, AcquisitionResult.Kind.NEGATIVE_EFFECT)) {
      AcquisitionResult.Entry timed =
          new AcquisitionResult.Entry(kind, "effect", 3, "Effect", "Effect for two turns", 2);
      AcquisitionResult.Entry untimed =
          new AcquisitionResult.Entry(kind, "effect", 3, "Effect", "See effect rules", null);
      assertEquals(3, timed.quantity());
      assertEquals(2, timed.durationTurns());
      assertNull(untimed.durationTurns());
      assertFalse(timed.isResourceChange());
      assertThrows(IllegalStateException.class, timed::delta);
      for (int duration : new int[] {0, -1}) {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AcquisitionResult.Entry(kind, "effect", 3, "Effect", "", duration));
      }
    }
  }

  @Test
  void oneSettledChoiceCanContainResourcesGoodsAndAnActualEffect() {
    List<AcquisitionResult.Entry> entries =
        new ArrayList<>(
            List.of(
                AcquisitionResult.Entry.gold(50),
                new AcquisitionResult.Entry(
                    AcquisitionResult.Kind.ITEM,
                    "test-curse-item",
                    1,
                    "Curse Item",
                    "Test item",
                    null),
                AcquisitionResult.Entry.hp(-10),
                new AcquisitionResult.Entry(
                    AcquisitionResult.Kind.CARD, "strike", 1, "Strike", "Deal damage", null),
                new AcquisitionResult.Entry(
                    AcquisitionResult.Kind.NEGATIVE_EFFECT,
                    "poison",
                    3,
                    "Poison",
                    "Three stacks",
                    2)));
    List<AcquisitionResult.Entry> expected = List.copyOf(entries);
    AcquisitionResult result = new AcquisitionResult("settled-event-choice", entries);
    entries.clear();

    assertEquals(expected, result.entries());
    assertEquals(AcquisitionResult.Kind.ITEM, result.entries().get(1).kind());
    assertEquals(-10, result.entries().get(2).delta());
    assertThrows(UnsupportedOperationException.class, result.entries()::clear);
  }

  @Test
  void resourceEntriesStillRequireValidIdentityAndDescription() {
    for (AcquisitionResult.Kind kind :
        List.of(AcquisitionResult.Kind.GOLD, AcquisitionResult.Kind.HP)) {
      assertThrows(
          IllegalArgumentException.class,
          () -> new AcquisitionResult.Entry(kind, " ", 10, "Resource", "", null));
      assertThrows(
          IllegalArgumentException.class,
          () -> new AcquisitionResult.Entry(kind, "resource", 10, " ", "", null));
      assertThrows(
          NullPointerException.class,
          () -> new AcquisitionResult.Entry(kind, "resource", 10, "Resource", null, null));
    }
  }
}

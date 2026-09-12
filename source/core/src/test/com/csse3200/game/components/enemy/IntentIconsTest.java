package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class IntentIconsTest {

  @Test
  void pathForCoversEveryIntentType() {
    for (IntentType type : IntentType.values()) {
      String path = IntentIcons.pathFor(type);
      assertTrue(path != null && path.endsWith(".png"), "bad path for " + type);
    }
  }

  @Test
  void pathForMapsEachTypeToADistinctIcon() {
    HashSet<String> seen = new HashSet<>();
    for (IntentType type : IntentType.values()) {
      seen.add(IntentIcons.pathFor(type));
    }

    assertEquals(IntentType.values().length, seen.size());
  }

  @Test
  void allListsEveryIconWithoutDuplicates() {
    String[] all = IntentIcons.all();

    assertEquals(IntentType.values().length, all.length);
    assertEquals(all.length, new HashSet<>(Arrays.asList(all)).size());
  }

  @Test
  void allReturnsADefensiveCopy() {
    IntentIcons.all()[0] = "mutated";

    assertEquals(IntentIcons.ATTACK, IntentIcons.all()[0]);
  }
}

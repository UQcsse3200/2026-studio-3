package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IntentIconsTest {

  @Test
  void shouldReturnAnIconForEveryIntentType() {
    for (IntentType type : IntentType.values()) {
      String path = IntentIcons.pathFor(type);

      assertTrue(path.startsWith("images/enemies/intents/"));
      assertTrue(path.endsWith(".png"));
    }
  }

  @Test
  void shouldReturnUniqueIconsForIntentTypes() {
    Set<String> seen = new HashSet<>();

    for (IntentType type : IntentType.values()) {
      seen.add(IntentIcons.pathFor(type));
    }

    assertEquals(IntentType.values().length, seen.size());
  }

  @Test
  void shouldReturnSilenceIconForSilenceDebuff() {
    assertEquals(
            IntentIcons.SILENCE,
            IntentIcons.pathFor(IntentType.DEBUFF, IntentEffectType.SILENCE));
  }

  @Test
  void shouldReturnDamageOnCardPlayIconForDamageOnCardPlayDebuff() {
    assertEquals(
            IntentIcons.DAMAGE_ON_CARD_PLAY,
            IntentIcons.pathFor(
                    IntentType.DEBUFF, IntentEffectType.DAMAGE_ON_CARD_PLAY));
  }

  @Test
  void shouldReturnGenericDebuffIconWhenEffectTypeIsNull() {
    assertEquals(
            IntentIcons.DEBUFF,
            IntentIcons.pathFor(IntentType.DEBUFF, null));
  }

  @Test
  void shouldIgnoreEffectTypeForNonDebuffIntents() {
    assertEquals(
            IntentIcons.ATTACK,
            IntentIcons.pathFor(IntentType.ATTACK, IntentEffectType.SILENCE));

    assertEquals(
            IntentIcons.DEFEND,
            IntentIcons.pathFor(
                    IntentType.DEFEND, IntentEffectType.DAMAGE_ON_CARD_PLAY));
  }

  @Test
  void shouldReturnUnknownIconWhenIntentTypeIsNull() {
    assertEquals(IntentIcons.UNKNOWN, IntentIcons.pathFor(null));
    assertEquals(
            IntentIcons.UNKNOWN,
            IntentIcons.pathFor(null, IntentEffectType.SILENCE));
  }

  @Test
  void shouldIncludeStatusSpecificIconsInAllAssets() {
    Set<String> allIcons = new HashSet<>(Arrays.asList(IntentIcons.all()));

    assertTrue(allIcons.contains(IntentIcons.SILENCE));
    assertTrue(allIcons.contains(IntentIcons.DAMAGE_ON_CARD_PLAY));
    assertTrue(allIcons.contains(IntentIcons.TAUNT));
  }

  @Test
  void shouldReturnDefensiveCopyOfAllIcons() {
    String[] firstResult = IntentIcons.all();
    String[] secondResult = IntentIcons.all();

    assertNotSame(firstResult, secondResult);

    firstResult[0] = "modified.png";

    assertEquals(IntentIcons.ATTACK, secondResult[0]);
    assertEquals(IntentIcons.ATTACK, IntentIcons.all()[0]);
  }

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
    String[] icons = IntentIcons.all();
    Set<String> uniqueIcons = new HashSet<>(Arrays.asList(icons));

    assertEquals(icons.length, uniqueIcons.size());
    assertEquals(8, icons.length);

    assertTrue(uniqueIcons.contains(IntentIcons.ATTACK));
    assertTrue(uniqueIcons.contains(IntentIcons.DEFEND));
    assertTrue(uniqueIcons.contains(IntentIcons.BUFF));
    assertTrue(uniqueIcons.contains(IntentIcons.DEBUFF));
    assertTrue(uniqueIcons.contains(IntentIcons.UNKNOWN));
    assertTrue(uniqueIcons.contains(IntentIcons.SILENCE));
    assertTrue(uniqueIcons.contains(IntentIcons.DAMAGE_ON_CARD_PLAY));
    assertTrue(uniqueIcons.contains(IntentIcons.TAUNT));
  }

  @Test
  void allReturnsADefensiveCopy() {
    IntentIcons.all()[0] = "mutated";

    assertEquals(IntentIcons.ATTACK, IntentIcons.all()[0]);
  }
}

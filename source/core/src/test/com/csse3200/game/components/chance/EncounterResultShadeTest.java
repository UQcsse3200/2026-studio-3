package com.csse3200.game.components.chance;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EncounterResultShadeTest {
  @Test
  void fieldIsSymmetricAndFeathersOutInBothDirections() {
    assertEquals(1f, EncounterResultShade.opacity(0f, 0f));
    assertEquals(0f, EncounterResultShade.opacity(1f, 0f));
    assertEquals(0f, EncounterResultShade.opacity(0f, 1f));
    assertTrue(EncounterResultShade.opacity(.5f, 0f) > EncounterResultShade.opacity(.85f, 0f));
    for (int i = 0; i <= 10; i++) {
      float x = i / 10f;
      assertEquals(
          EncounterResultShade.opacity(x, .4f), EncounterResultShade.opacity(-x, -.4f), .0001f);
      assertTrue(EncounterResultShade.opacity(x, .8f) <= EncounterResultShade.opacity(x, .2f));
    }
  }
}

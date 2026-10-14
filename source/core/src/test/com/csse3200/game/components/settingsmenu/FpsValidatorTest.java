package com.csse3200.game.components.settingsmenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class FpsValidatorTest {
  @Test
  void rejectsNonPositiveValues() {
    for (String value : new String[] {"-1", "-60", "-2147483648", "0", "000", "-0"}) {
      assertNull(FpsValidator.parse(value), value);
    }
  }

  @Test
  void rejectsInvalidText() {
    for (String value :
        new String[] {"", " ", "\t\n", "abc", "60fps", "60.5", "60.0", "+60", "6 0", "1e2"}) {
      assertNull(FpsValidator.parse(value), value);
    }
    assertNull(FpsValidator.parse(null));
  }

  @Test
  void rejectsIntegerOverflow() {
    assertNull(FpsValidator.parse("2147483648"));
    assertNull(FpsValidator.parse("999999999999999999999999999999"));
  }

  @Test
  void acceptsPositiveIntegersIncludingBoundaries() {
    for (int value : new int[] {1, 30, 60, 120, Integer.MAX_VALUE}) {
      assertEquals(Integer.valueOf(value), FpsValidator.parse(Integer.toString(value)));
    }
  }

  @Test
  void acceptsLeadingZerosAndSurroundingWhitespace() {
    assertEquals(Integer.valueOf(60), FpsValidator.parse("0060"));
    assertEquals(Integer.valueOf(120), FpsValidator.parse(" \t120\n "));
  }
}

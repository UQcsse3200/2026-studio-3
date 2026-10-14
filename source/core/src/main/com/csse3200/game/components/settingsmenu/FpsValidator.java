package com.csse3200.game.components.settingsmenu;

/** Validates the FPS cap entered in the settings form. */
final class FpsValidator {
  private FpsValidator() {}

  /** Returns a positive integer, or null for invalid input or integer overflow. */
  static Integer parse(String input) {
    if (input == null) {
      return null;
    }

    String value = input.trim();

    if (!value.matches("[0-9]+")) {
      return null;
    }

    try {
      int fps = Integer.parseInt(value);
      return fps > 0 ? fps : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }
}

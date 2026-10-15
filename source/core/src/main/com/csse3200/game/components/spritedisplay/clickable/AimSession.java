package com.csse3200.game.components.spritedisplay.clickable;

import com.badlogic.gdx.math.Vector2;

/** Drag source contract for displaying aim and resolving the selected target. */
public interface AimSession {
  void begin(Vector2 cardPosition, Vector2 pointer);

  void update(Vector2 pointer);

  String release(Vector2 pointer);

  void cancel();

  /** Whether to keep the selected card in hand and show an arrow instead of a cursor card. */
  default boolean usesTargetingArrow() {
    return false;
  }
}

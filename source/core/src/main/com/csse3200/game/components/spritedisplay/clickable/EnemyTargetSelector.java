package com.csse3200.game.components.spritedisplay.clickable;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.Map;
import java.util.function.Predicate;

/** Selects the nearest eligible enemy to a pointer in stage coordinates. */
public final class EnemyTargetSelector {
  private EnemyTargetSelector() {}

  public static String select(
      Map<String, Rectangle> bounds,
      Vector2 pointer,
      Predicate<String> isAvailable,
      float snapDistance) {
    String selected = null;
    float bestDistance = snapDistance * snapDistance;
    float bestCenterDistance = Float.MAX_VALUE;
    for (Map.Entry<String, Rectangle> entry : bounds.entrySet()) {
      if (!isAvailable.test(entry.getKey())) {
        continue;
      }
      Rectangle box = entry.getValue();
      float dx = Math.max(box.x - pointer.x, Math.max(0, pointer.x - (box.x + box.width)));
      float dy = Math.max(box.y - pointer.y, Math.max(0, pointer.y - (box.y + box.height)));
      float distance = dx * dx + dy * dy;
      float centerDx = box.x + box.width / 2f - pointer.x;
      float centerDy = box.y + box.height / 2f - pointer.y;
      float centerDistance = centerDx * centerDx + centerDy * centerDy;
      if (distance <= bestDistance
          && (selected == null
              || distance < bestDistance
              || centerDistance < bestCenterDistance)) {
        selected = entry.getKey();
        bestDistance = distance;
        bestCenterDistance = centerDistance;
      }
    }
    return selected;
  }
}

package com.csse3200.game.components.spritedisplay.clickable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EnemyTargetSelectorTest {
  private final Map<String, Rectangle> enemies = new LinkedHashMap<>();

  @Test
  void cursorInsideEnemySelectsThatInstance() {
    enemies.put("10", new Rectangle(100, 100, 80, 100));
    enemies.put("11", new Rectangle(260, 100, 80, 100));

    assertEquals("11", EnemyTargetSelector.select(enemies, new Vector2(300, 150), id -> true, 90));
  }

  @Test
  void cursorNearEnemySnapsAndSwitchesAsItMoves() {
    enemies.put("10", new Rectangle(100, 100, 80, 100));
    enemies.put("11", new Rectangle(260, 100, 80, 100));

    assertEquals("10", EnemyTargetSelector.select(enemies, new Vector2(190, 150), id -> true, 90));
    assertEquals("11", EnemyTargetSelector.select(enemies, new Vector2(250, 150), id -> true, 90));
  }

  @Test
  void deadEnemiesAndDistantCursorCannotBeSelected() {
    enemies.put("10", new Rectangle(100, 100, 80, 100));
    enemies.put("11", new Rectangle(260, 100, 80, 100));

    assertNull(EnemyTargetSelector.select(enemies, new Vector2(140, 150), id -> !id.equals("10"), 90));
    assertNull(EnemyTargetSelector.select(enemies, new Vector2(500, 150), id -> true, 90));
  }
}

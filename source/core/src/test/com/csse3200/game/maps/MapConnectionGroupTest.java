package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class MapConnectionGroupTest {

  @BeforeEach
  void setUp() {}

  @Test
  void calculateHorizontalConnection() {
    Vector2 start = new Vector2(0, 0);
    Vector2 end = new Vector2(100, 0);

    MapConnectionGroup connection = new MapConnectionGroup(start, end, true);

    assertEquals(-90, connection.getAngle(), 0.1);
  }

  @Test
  void calculateDiagonalConnection() {
    Vector2 start = new Vector2(0, 0);
    Vector2 end = new Vector2(100, 100);

    MapConnectionGroup connection = new MapConnectionGroup(start, end, true);

    assertEquals(-45, connection.getAngle(), 0.1);
  }
}

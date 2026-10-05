package com.csse3200.game;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.Screen;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.RoomType;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class GdxGameTest {
  @Test
  void shouldRejectBlankEventIdBeforeChangingScreen() {
    GdxGame game = gameWithActiveNode(RoomType.EVENT);
    Screen screen = mock(Screen.class);
    game.setScreen(screen);

    assertThrows(IllegalArgumentException.class, () -> game.startEvent(" "));
    assertThrows(IllegalArgumentException.class, () -> game.startEvent(null));
    assertSame(screen, game.getScreen());
    verify(screen, never()).dispose();
  }

  @Test
  void shouldRequireAnActiveEventNodeBeforeChangingScreen() {
    GdxGame game = new GdxGame();
    Screen screen = mock(Screen.class);
    game.setScreen(screen);

    assertThrows(IllegalStateException.class, () -> game.startEvent("dice-game"));
    assertSame(screen, game.getScreen());
    verify(screen, never()).dispose();
  }

  @Test
  void shouldRejectShopNodeBeforeChangingScreen() {
    GdxGame game = gameWithActiveNode(RoomType.SHOP);
    Screen screen = mock(Screen.class);
    game.setScreen(screen);

    assertThrows(IllegalStateException.class, () -> game.startEvent("dice-game"));
    assertSame(screen, game.getScreen());
    verify(screen, never()).dispose();
  }

  @Test
  void shouldRejectUnknownEventIdBeforeChangingScreen() {
    GdxGame game = gameWithActiveNode(RoomType.EVENT);
    Screen screen = mock(Screen.class);
    game.setScreen(screen);

    assertThrows(IllegalArgumentException.class, () -> game.startEvent("healing-spring"));
    assertSame(screen, game.getScreen());
    verify(screen, never()).dispose();
  }

  private static GdxGame gameWithActiveNode(RoomType roomType) {
    GdxGame game = new GdxGame();
    MapNode node = new MapNode(7, roomType);
    game.getRunState().restoreRun(new MapGraph(Map.of(7, node), false), 7);
    return game;
  }
}

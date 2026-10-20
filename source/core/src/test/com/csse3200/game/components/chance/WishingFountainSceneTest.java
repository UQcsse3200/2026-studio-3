package com.csse3200.game.components.chance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class WishingFountainSceneTest {
  @Test
  void idleAndReleaseCrossIntersectionsStayAlignedWhenResized() {
    WishingFountainScene scene = new WishingFountainScene(UIComponent.getSharedSkin());
    AtomicInteger completions = new AtomicInteger();
    assertTrue(scene.playWishAnimation(completions::incrementAndGet));
    assertFalse(scene.playWishAnimation(completions::incrementAndGet));
    scene.act(1.2f);
    for (float[] size :
        List.of(new float[] {1280, 800}, new float[] {1600, 1000}, new float[] {1600, 900})) {
      scene.setSize(size[0], size[1]);
      Batch batch = mock(Batch.class);
      when(batch.getColor()).thenReturn(new Color(Color.WHITE));
      List<float[]> horizontal = new ArrayList<>();
      doAnswer(
              call -> {
                float width = call.getArgument(3);
                float height = call.getArgument(4);
                float scale = Math.max(size[0] / 1280f, size[1] / 800f);
                if (Math.abs(height - 4 * scale) < .01f
                    && (Math.abs(width - 28 * scale) < .01f
                        || Math.abs(width - 68 * scale) < .01f)) {
                  horizontal.add(
                      new float[] {
                        (float) call.getArgument(1) + width / 2,
                        (float) call.getArgument(2) + height / 2
                      });
                }
                return null;
              })
          .when(batch)
          .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
      scene.findActor("fountain-cross").draw(batch, 1f);
      scene.findActor("fountain-wish-effect").draw(batch, 1f);
      assertEquals(2, horizontal.size());
      assertArrayEquals(horizontal.get(0), horizontal.get(1), .01f);
    }
    scene.act(.6f);
    scene.act(1f);
    assertEquals(1, completions.get());
  }
}

package com.csse3200.game.components.gamearea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CombatBackgroundComponentTest {
  @Test
  void shouldFillCameraView() {
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(200);
    when(texture.getHeight()).thenReturn(100);

    OrthographicCamera camera = new OrthographicCamera();
    camera.viewportWidth = 20f;
    camera.viewportHeight = 10f;
    camera.position.set(10f, 5f, 0f);
    SpriteBatch batch = mock(SpriteBatch.class);

    CombatBackgroundComponent background = new CombatBackgroundComponent(texture, camera);
    background.draw(batch);

    verify(batch).draw(texture, 0f, 0f, 20f, 10f);
  }

  @Test
  void shouldKeepWideImageCentred() {
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(400);
    when(texture.getHeight()).thenReturn(100);

    OrthographicCamera camera = new OrthographicCamera();
    camera.viewportWidth = 20f;
    camera.viewportHeight = 10f;
    camera.position.set(10f, 5f, 0f);
    SpriteBatch batch = mock(SpriteBatch.class);

    CombatBackgroundComponent background = new CombatBackgroundComponent(texture, camera);
    background.draw(batch);

    verify(batch).draw(texture, -10f, 0f, 40f, 10f);
  }

  @Test
  void shouldUseBackgroundLayer() {
    Texture texture = mock(Texture.class);
    OrthographicCamera camera = new OrthographicCamera();
    CombatBackgroundComponent background = new CombatBackgroundComponent(texture, camera);

    assertEquals(0, background.getLayer());
    assertEquals(1f, background.getZIndex());
  }
}

package com.csse3200.game.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ItemIconViewTest {
  @Test
  void showsTheCompleteBakedFrameWithoutCroppingOrAddingAnotherOne() {
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(96);
    when(texture.getHeight()).thenReturn(96);

    ItemIconView icon = new ItemIconView();
    icon.setSize(56f, 56f);
    icon.setItem(texture);

    TextureRegionDrawable drawable = (TextureRegionDrawable) icon.getArtwork().getDrawable();
    assertEquals(0, drawable.getRegion().getRegionX());
    assertEquals(0, drawable.getRegion().getRegionY());
    assertEquals(96, drawable.getRegion().getRegionWidth());
    assertEquals(96, drawable.getRegion().getRegionHeight());
    assertEquals(56f, icon.getWidth());
    assertEquals(56f, icon.getHeight());
    assertTrue(icon.getArtwork().isVisible());

    icon.setItem(null);
    assertFalse(icon.getArtwork().isVisible());
  }
}

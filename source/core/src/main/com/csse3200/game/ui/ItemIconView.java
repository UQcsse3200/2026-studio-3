package com.csse3200.game.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;

/** Displays an item texture as supplied, with no extra border drawn by the UI. */
public class ItemIconView extends Stack {
  private final Image artwork = new Image();

  public ItemIconView() {
    setName("itemIconView");
    artwork.setScaling(Scaling.fit);
    add(artwork);
  }

  public void setItem(Texture texture) {
    if (texture == null) {
      artwork.setDrawable(null);
      artwork.setVisible(false);
      return;
    }
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    artwork.setDrawable(new TextureRegionDrawable(texture));
    artwork.setVisible(true);
  }

  public Image getArtwork() {
    return artwork;
  }
}

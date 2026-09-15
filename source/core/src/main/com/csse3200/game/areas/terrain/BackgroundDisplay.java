package com.csse3200.game.areas.terrain;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Renders in the image for the background */
public class BackgroundDisplay extends RenderComponent {
  private Texture background;

  @Override
  public void create() {
    super.create();

    background =
        ServiceLocator.getResourceService().getAsset("images/battle_background.png", Texture.class);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    batch.draw(background, -3f, 0, 21f, 15f);
  }

  @Override
  public float getZIndex() {
    return 0;
  }

  @Override
  public int getLayer() {
    return 0;
  }

  @Override
  public void dispose() {
    super.dispose();
  }
}

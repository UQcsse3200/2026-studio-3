package com.csse3200.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/** Image button states with a stable click area. */
public final class PixelButtonStyles {
  private PixelButtonStyles() {}

  public static TextButtonStyle create(Skin skin, Texture texture) {
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    TextureRegionDrawable image = new TextureRegionDrawable(new TextureRegion(texture));

    TextButtonStyle style = new TextButtonStyle();
    style.font = skin.get(TextButtonStyle.class).font;
    style.up = new FeedbackDrawable(image.tint(new Color(0.85f, 0.85f, 0.85f, 1f)), 1f, 0f);
    style.over = new FeedbackDrawable(image, 1.125f, 0f);
    style.down = new FeedbackDrawable(image.tint(new Color(0.65f, 0.65f, 0.65f, 1f)), 0.9375f, -2f);
    style.fontColor = Color.valueOf("E8D6AE");
    style.overFontColor = Color.valueOf("FFF0B8");
    style.downFontColor = Color.valueOf("D8BD91");
    style.pressedOffsetY = -2f;
    return style;
  }

  private static final class FeedbackDrawable extends BaseDrawable {
    private final Drawable image;
    private final float scale;
    private final float offsetY;

    private FeedbackDrawable(Drawable image, float scale, float offsetY) {
      this.image = image;
      this.scale = scale;
      this.offsetY = offsetY;
    }

    @Override
    public void draw(Batch batch, float x, float y, float width, float height) {
      float drawWidth = width * scale;
      float drawHeight = height * scale;
      image.draw(
          batch,
          x + (width - drawWidth) / 2f,
          y + (height - drawHeight) / 2f + offsetY,
          drawWidth,
          drawHeight);
    }
  }
}

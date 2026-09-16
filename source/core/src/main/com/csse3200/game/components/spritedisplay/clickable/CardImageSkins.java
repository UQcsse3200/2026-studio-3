package com.csse3200.game.components.spritedisplay.clickable;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import java.util.HashMap;
import java.util.Map;

/**
 * Caches a one-off {@link Skin} per card texture path, wrapping the texture as an {@link
 * ImageButton.ImageButtonStyle} with that texture as its {@code imageUp} drawable. Shared by any
 * {@link ClickableFactory} caller that renders a card as a plain image button (the hand row, the
 * deck-rearrange popup), so the same texture path is only ever loaded once.
 */
public final class CardImageSkins {
  private static final Map<String, Skin> CACHE = new HashMap<>();

  private CardImageSkins() {}

  public static Skin forTexturePath(String texturePath) {
    return CACHE.computeIfAbsent(
        texturePath,
        path -> {
          Texture texture = new Texture(Gdx.files.internal(path));
          TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));

          ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
          style.imageUp = drawable;

          Skin skin = new Skin();
          skin.add("default", style, ImageButton.ImageButtonStyle.class);
          return skin;
        });
  }
}

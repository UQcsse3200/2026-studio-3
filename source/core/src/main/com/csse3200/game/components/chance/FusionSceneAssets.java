package com.csse3200.game.components.chance;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.components.shop.ShopDisplay;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;

/** Visual assets shared by the Card Fusion introduction and delegated selection view. */
final class FusionSceneAssets {
  static final float WIDTH = 1280f;
  static final float HEIGHT = 800f;

  private FusionSceneAssets() {}

  static Image background(Skin skin) {
    ResourceService resources = ServiceLocator.getResourceService();
    Image image;
    if (resources != null
        && resources.containsAsset(
            ChanceEncounterDisplay.FUSION_BACKGROUND_TEXTURE, Texture.class)) {
      Texture texture =
          resources.getAsset(ChanceEncounterDisplay.FUSION_BACKGROUND_TEXTURE, Texture.class);
      texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      image = new Image(new TextureRegionDrawable(new TextureRegion(texture)));
      image.setScaling(Scaling.fill);
      image.setColor(0.74f, 0.72f, 0.75f, 1f);
    } else {
      image = new Image(skin.newDrawable("white", new Color(0.12f, 0.10f, 0.16f, 1f)));
    }
    image.setBounds(0f, 0f, WIDTH, HEIGHT);
    image.setTouchable(Touchable.disabled);
    return image;
  }

  static Image cardArt(Skin skin, CardService cards, String cardId) {
    ResourceService resources = ServiceLocator.getResourceService();
    String path = cards.getCard(cardId).map(card -> card.texturePath).orElse(null);
    Image image;
    if (path != null && resources != null && resources.containsAsset(path, Texture.class)) {
      Texture texture = resources.getAsset(path, Texture.class);
      texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      image = new Image(new TextureRegionDrawable(new TextureRegion(texture)));
      image.setScaling(Scaling.fit);
    } else {
      image = new Image(skin.newDrawable("white", new Color(0.22f, 0.23f, 0.32f, 1f)));
    }
    image.setTouchable(Touchable.disabled);
    return image;
  }

  static Image cardBack(Skin skin) {
    ResourceService resources = ServiceLocator.getResourceService();
    Image image;
    if (resources != null
        && resources.containsAsset(
            ChanceEncounterDisplay.FUSION_CARD_BACK_TEXTURE, Texture.class)) {
      Texture texture =
          resources.getAsset(ChanceEncounterDisplay.FUSION_CARD_BACK_TEXTURE, Texture.class);
      texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      image = new Image(new TextureRegionDrawable(new TextureRegion(texture)));
      image.setScaling(Scaling.fit);
    } else {
      image = new Image(skin.newDrawable("white", new Color(0.18f, 0.15f, 0.21f, 1f)));
    }
    image.setTouchable(Touchable.disabled);
    return image;
  }

  static Drawable plaque(Skin skin, Color tint) {
    ResourceService resources = ServiceLocator.getResourceService();
    if (resources == null
        || !resources.containsAsset(ShopDisplay.PLAQUE_FRAME_TEXTURE, Texture.class)) {
      return skin.newDrawable("white", tint);
    }
    Texture texture = resources.getAsset(ShopDisplay.PLAQUE_FRAME_TEXTURE, Texture.class);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    NinePatch patch =
        new NinePatch(new TextureRegion(texture, 72, 152, 2031, 409), 105, 105, 78, 78);
    patch.scale(0.13f, 0.13f);
    patch.setColor(tint);
    return new NinePatchDrawable(patch);
  }

  enum FrameKind {
    TITLE,
    INSCRIPTION,
    DISPLAY,
    CARD,
    CARD_GLOW,
    REWARD_CARD,
    REWARD_ART,
    STATUS,
    SLOT,
    BUTTON
  }

  static Drawable pixelFrame(Skin skin, Color fill, Color edge, FrameKind kind) {
    return new PixelFrameDrawable(skin, fill, edge, kind);
  }

  /** A low-alpha, pixel-stepped pool of forge light; no extra texture or shader is required. */
  static Image warmGlow(Skin skin, float strength) {
    Image glow = new Image(new WarmGlowDrawable(skin, strength));
    glow.setTouchable(Touchable.disabled);
    return glow;
  }

  private static final class WarmGlowDrawable extends BaseDrawable {
    private static final int COLUMNS = 17;
    private static final int ROWS = 9;
    private final Drawable[] levels = new Drawable[7];
    private final int[][] samples = new int[ROWS][COLUMNS];

    private WarmGlowDrawable(Skin skin, float strength) {
      for (int level = 1; level < levels.length; level++) {
        levels[level] =
            skin.newDrawable("white", new Color(1f, 0.56f, 0.22f, strength * level / 6f));
      }
      for (int row = 0; row < ROWS; row++) {
        for (int column = 0; column < COLUMNS; column++) {
          float dx = (column + 0.5f) * 2f / COLUMNS - 1f;
          float dy = (row + 0.5f) * 2f / ROWS - 1f;
          float falloff = Math.max(0f, 1f - dx * dx - dy * dy);
          samples[row][column] = Math.min(6, (int) (falloff * falloff * 7f));
        }
      }
    }

    @Override
    public void draw(Batch batch, float x, float y, float width, float height) {
      float cellWidth = width / COLUMNS;
      float cellHeight = height / ROWS;
      for (int row = 0; row < ROWS; row++) {
        for (int column = 0; column < COLUMNS; column++) {
          int level = samples[row][column];
          if (level > 0) {
            levels[level].draw(
                batch, x + column * cellWidth, y + row * cellHeight, cellWidth, cellHeight);
          }
        }
      }
    }
  }

  /** Scalable, texture-free pixel metalwork used only by the Card Fusion selection controls. */
  private static final class PixelFrameDrawable extends BaseDrawable {
    private final Drawable fill;
    private final Drawable edge;
    private final Drawable innerEdge;
    private final Drawable accent;
    private final Drawable forgeAccent;
    private final Drawable darkMetal;
    private final FrameKind kind;

    private PixelFrameDrawable(Skin skin, Color fillColor, Color edgeColor, FrameKind kind) {
      fill = fillColor == null ? null : skin.newDrawable("white", fillColor);
      edge = skin.newDrawable("white", edgeColor);
      innerEdge = skin.newDrawable("white", new Color(edgeColor).mul(0.65f, 0.65f, 0.65f, 0.72f));
      accent = skin.newDrawable("white", new Color(edgeColor).mul(1.18f, 1.13f, 1.04f, 1f));
      forgeAccent =
          skin.newDrawable(
              "white",
              kind == FrameKind.REWARD_CARD
                  ? new Color(0.94f, 0.46f, 0.15f, 0.95f)
                  : new Color(0.68f, 0.34f, 0.15f, 0.9f));
      darkMetal = skin.newDrawable("white", new Color(0.13f, 0.11f, 0.13f, 1f));
      this.kind = kind;
    }

    @Override
    public void draw(Batch batch, float x, float y, float width, float height) {
      float cut =
          switch (kind) {
            case TITLE, DISPLAY, BUTTON -> 10f;
            case STATUS -> 8f;
            case INSCRIPTION, CARD, CARD_GLOW -> 6f;
            case REWARD_CARD -> 8f;
            case REWARD_ART -> 3f;
            case SLOT -> 4f;
          };
      cut = Math.min(cut, Math.min(width, height) / 5f);
      if (fill != null) {
        fill.draw(batch, x + cut, y, width - 2f * cut, height);
        fill.draw(batch, x, y + cut, cut, height - 2f * cut);
        fill.draw(batch, x + width - cut, y + cut, cut, height - 2f * cut);
        for (float step = 0f; step < cut; step += 2f) {
          float span = Math.min(2f, cut - step);
          fill.draw(batch, x + step, y + cut - step - span, cut - step, span);
          fill.draw(batch, x + width - cut, y + cut - step - span, cut - step, span);
          fill.draw(batch, x + step, y + height - cut + step, cut - step, span);
          fill.draw(batch, x + width - cut, y + height - cut + step, cut - step, span);
        }
      }
      outline(batch, edge, x, y, width, height, cut, kind == FrameKind.REWARD_ART ? 1f : 2f);
      if (kind != FrameKind.SLOT && kind != FrameKind.CARD_GLOW && kind != FrameKind.REWARD_ART) {
        outline(batch, innerEdge, x + 4f, y + 4f, width - 8f, height - 8f, cut - 2f, 1f);
      }
      if (kind == FrameKind.CARD_GLOW) {
        outline(batch, innerEdge, x + 3f, y + 3f, width - 6f, height - 6f, cut - 1f, 1f);
      }
      if (kind != FrameKind.CARD_GLOW && kind != FrameKind.REWARD_ART) {
        cornerRivet(batch, x + cut, y + cut);
        cornerRivet(batch, x + width - cut, y + cut);
        cornerRivet(batch, x + cut, y + height - cut);
        cornerRivet(batch, x + width - cut, y + height - cut);
      }
      switch (kind) {
        case TITLE, DISPLAY -> {
          diamond(batch, x + width / 2f, y + height - 2f, 4f);
          diamond(batch, x + width / 2f, y + 2f, 4f);
          if (kind == FrameKind.DISPLAY) {
            diamond(batch, x + 2f, y + height / 2f, 3f);
            diamond(batch, x + width - 2f, y + height / 2f, 3f);
          }
        }
        case INSCRIPTION -> {
          diamond(batch, x + 14f, y + height / 2f, 3f);
          diamond(batch, x + width - 14f, y + height / 2f, 3f);
          diamond(batch, x + width / 2f, y + 1f, 2f);
        }
        case CARD -> {
          innerEdge.draw(batch, x + 12f, y + 44f, width - 24f, 1f);
          diamond(batch, x + width / 2f, y + 44.5f, 2f);
        }
        case REWARD_CARD -> {
          rewardCorner(batch, x, y, width, height, false, false);
          rewardCorner(batch, x, y, width, height, true, false);
          rewardCorner(batch, x, y, width, height, false, true);
          rewardCorner(batch, x, y, width, height, true, true);
          rewardEdge(batch, x, y, width, height);
          rewardJewel(batch, x + width / 2f, y + height - 2f, 10f);
          rewardJewel(batch, x + width / 2f, y + 2f, 8f);
          rewardJewel(batch, x + 2f, y + height / 2f, 6f);
          rewardJewel(batch, x + width - 2f, y + height / 2f, 6f);
        }
        case REWARD_ART -> {
          artCorner(batch, x, y, width, height, false, false);
          artCorner(batch, x, y, width, height, true, false);
          artCorner(batch, x, y, width, height, false, true);
          artCorner(batch, x, y, width, height, true, true);
          diamond(batch, x + width / 2f, y + 1f, 2f);
        }
        case STATUS -> {
          diamond(batch, x + width / 2f, y + height - 2f, 3f);
          diamond(batch, x + width / 2f, y + 2f, 3f);
        }
        case BUTTON -> {
          accent.draw(batch, x + 15f, y + height / 2f - 2f, 2f, 4f);
          accent.draw(batch, x + width - 17f, y + height / 2f - 2f, 2f, 4f);
        }
        case SLOT, CARD_GLOW -> {
          // The small slots and selected overlay stay deliberately understated.
        }
      }
    }

    private void outline(
        Batch batch,
        Drawable ink,
        float x,
        float y,
        float width,
        float height,
        float cut,
        float weight) {
      ink.draw(batch, x + cut, y, width - 2f * cut, weight);
      ink.draw(batch, x + cut, y + height - weight, width - 2f * cut, weight);
      ink.draw(batch, x, y + cut, weight, height - 2f * cut);
      ink.draw(batch, x + width - weight, y + cut, weight, height - 2f * cut);
      for (float step = 0f; step < cut; step += 2f) {
        float span = Math.min(2f, cut - step);
        ink.draw(batch, x + step, y + cut - step - span, weight, span);
        ink.draw(batch, x + width - step - weight, y + cut - step - span, weight, span);
        ink.draw(batch, x + step, y + height - cut + step, weight, span);
        ink.draw(batch, x + width - step - weight, y + height - cut + step, weight, span);
      }
    }

    private void cornerRivet(Batch batch, float x, float y) {
      accent.draw(batch, x - 1f, y - 1f, 2f, 2f);
    }

    private void rewardCorner(
        Batch batch, float x, float y, float width, float height, boolean right, boolean top) {
      float cornerX = right ? x + width - 11f : x + 1f;
      float cornerY = top ? y + height - 11f : y + 1f;
      darkMetal.draw(batch, cornerX - 2f, cornerY - 2f, 14f, 14f);
      accent.draw(batch, cornerX + 2f, cornerY + 2f, 8f, 1f);
      accent.draw(batch, cornerX + 2f, cornerY + 9f, 8f, 1f);
      accent.draw(batch, cornerX + 2f, cornerY + 2f, 1f, 8f);
      accent.draw(batch, cornerX + 9f, cornerY + 2f, 1f, 8f);
      forgeAccent.draw(batch, cornerX + 5f, cornerY + 5f, 3f, 3f);
      float flangeX = right ? cornerX - 18f : cornerX + 11f;
      float flangeY = top ? cornerY + 9f : cornerY + 2f;
      accent.draw(batch, flangeX, flangeY, 18f, 1f);
      forgeAccent.draw(batch, right ? flangeX + 3f : flangeX + 12f, flangeY, 4f, 2f);
      accent.draw(
          batch, right ? cornerX + 9f : cornerX + 2f, top ? cornerY - 18f : cornerY + 11f, 1f, 18f);
      forgeAccent.draw(
          batch, right ? cornerX + 9f : cornerX + 2f, top ? cornerY - 7f : cornerY + 14f, 2f, 4f);
    }

    private void rewardEdge(Batch batch, float x, float y, float width, float height) {
      float center = x + width / 2f;
      accent.draw(batch, center - 61f, y + height - 2f, 47f, 1f);
      accent.draw(batch, center + 14f, y + height - 2f, 47f, 1f);
      forgeAccent.draw(batch, center - 63f, y + height - 3f, 5f, 2f);
      forgeAccent.draw(batch, center + 58f, y + height - 3f, 5f, 2f);
      forgeAccent.draw(batch, x + 29f, y + height - 2f, 15f, 1f);
      forgeAccent.draw(batch, x + width - 44f, y + height - 2f, 15f, 1f);
      accent.draw(batch, center - 25f, y + 1f, 11f, 1f);
      accent.draw(batch, center + 14f, y + 1f, 11f, 1f);
      forgeAccent.draw(batch, x + 1f, y + height / 2f - 24f, 1f, 10f);
      forgeAccent.draw(batch, x + width - 2f, y + height / 2f + 14f, 1f, 10f);
      forgeAccent.draw(batch, x + 1f, y + height * 0.25f, 1f, 17f);
      forgeAccent.draw(batch, x + width - 2f, y + height * 0.75f - 17f, 1f, 17f);
    }

    private void rewardJewel(Batch batch, float centerX, float centerY, float radius) {
      for (float row = -radius; row <= radius; row += 2f) {
        float halfWidth = radius - Math.abs(row) + 1f;
        darkMetal.draw(batch, centerX - halfWidth, centerY + row, halfWidth * 2f, 2f);
      }
      float inner = radius - 2f;
      for (float row = -inner; row <= inner; row += 2f) {
        float halfWidth = inner - Math.abs(row) + 1f;
        accent.draw(batch, centerX - halfWidth, centerY + row, halfWidth * 2f, 2f);
      }
      forgeAccent.draw(batch, centerX - 2f, centerY - 2f, 4f, 4f);
    }

    private void artCorner(
        Batch batch, float x, float y, float width, float height, boolean right, boolean top) {
      float edgeX = right ? x + width - 5f : x + 4f;
      float edgeY = top ? y + height - 5f : y + 4f;
      accent.draw(batch, right ? edgeX - 6f : edgeX, edgeY, 7f, 1f);
      accent.draw(batch, edgeX, top ? edgeY - 6f : edgeY, 1f, 7f);
      forgeAccent.draw(batch, edgeX, edgeY, 2f, 2f);
    }

    private void diamond(Batch batch, float centerX, float centerY, float radius) {
      for (float row = -radius; row <= radius; row += 2f) {
        float halfWidth = radius - Math.abs(row) + 1f;
        accent.draw(batch, centerX - halfWidth, centerY + row, halfWidth * 2f, 2f);
      }
    }
  }

  static TextButtonStyle buttonStyle(Skin skin, boolean primary) {
    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));
    Color base =
        primary ? new Color(0.55f, 0.40f, 0.31f, 0.97f) : new Color(0.38f, 0.35f, 0.43f, 0.97f);
    style.up = plaque(skin, base);
    style.over = plaque(skin, new Color(base).mul(1.12f, 1.12f, 1.12f, 1f));
    style.down = plaque(skin, new Color(base).mul(0.82f, 0.82f, 0.82f, 1f));
    style.disabled = plaque(skin, new Color(0.29f, 0.28f, 0.32f, 0.9f));
    style.fontColor = new Color(0.92f, 0.87f, 0.77f, 1f);
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.WHITE;
    style.disabledFontColor = new Color(0.61f, 0.57f, 0.54f, 1f);
    return style;
  }

  static TextButton button(String text, Skin skin, boolean primary) {
    TextButton button = new TextButton(text, buttonStyle(skin, primary));
    button.getLabel().setWrap(true);
    return button;
  }
}

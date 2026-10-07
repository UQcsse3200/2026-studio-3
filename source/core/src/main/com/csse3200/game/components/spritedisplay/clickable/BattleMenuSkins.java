package com.csse3200.game.components.spritedisplay.clickable;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Builds the gothic pixel skins used by the three permanent battle menu buttons.
 *
 * <p>The icon sheet is deliberately kept separate from the scalable frame. This lets the frame use
 * a nine-patch without blurring or stretching the chunky icon pixels.
 */
public final class BattleMenuSkins {
  public enum Icon {
    CARD(0),
    INVENTORY(1),
    END_TURN(2);

    private final int sheetIndex;

    Icon(int sheetIndex) {
      this.sheetIndex = sheetIndex;
    }
  }

  private static final String ICON_SHEET = "images/ui/battle-menu-icons.png";
  private static final String DEFAULT_STYLE = "default";
  private static final int FRAME_SIZE = 32;
  private static final int FRAME_SPLIT = 10;
  private static final float ICON_SIZE = 48f;
  private static final Map<Icon, Skin> CACHE = new EnumMap<>(Icon.class);

  private static Skin baseSkin;
  private static Texture iconSheet;
  private static NinePatchDrawable up;
  private static NinePatchDrawable over;
  private static NinePatchDrawable down;

  private BattleMenuSkins() {}

  public static Skin forIcon(Icon icon) {
    return CACHE.computeIfAbsent(icon, BattleMenuSkins::createSkin);
  }

  private static Skin createSkin(Icon icon) {
    ensureSharedResources();

    if (iconSheet == null) {
      iconSheet = new Texture(Gdx.files.internal(ICON_SHEET));
    }
    iconSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    int regionWidth = iconSheet.getWidth() / Icon.values().length;
    TextureRegion region =
        new TextureRegion(
            iconSheet, icon.sheetIndex * regionWidth, 0, regionWidth, iconSheet.getHeight());
    TextureRegionDrawable iconDrawable = new TextureRegionDrawable(region);
    iconDrawable.setMinSize(ICON_SIZE, ICON_SIZE);

    BitmapFont font = baseSkin.getFont("button");
    ImageTextButton.ImageTextButtonStyle style = new ImageTextButton.ImageTextButtonStyle();
    style.up = up;
    style.over = over;
    style.down = down;
    style.font = font;
    style.fontColor = Color.valueOf("E8C894");
    style.overFontColor = Color.valueOf("FFE3A0");
    style.downFontColor = Color.valueOf("C68A3A");
    style.imageUp = iconDrawable;
    style.imageOver = iconDrawable.tint(Color.valueOf("FFF0BA"));
    style.imageDown = iconDrawable.tint(Color.valueOf("B97832"));
    style.pressedOffsetY = -2f;

    Skin skin = new Skin();
    skin.add(DEFAULT_STYLE, style, ImageTextButton.ImageTextButtonStyle.class);
    return skin;
  }

  private static void ensureSharedResources() {
    if (baseSkin == null) {
      baseSkin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    }
    if (up == null) {
      up = createFrame(Color.valueOf("11131E"), Color.valueOf("8B542C"), Color.valueOf("E2A44A"));
      over = createFrame(Color.valueOf("191B28"), Color.valueOf("B16C34"), Color.valueOf("FFD071"));
      down = createFrame(Color.valueOf("090B12"), Color.valueOf("704021"), Color.valueOf("B97932"));
    }
  }

  private static NinePatchDrawable createFrame(Color face, Color bronze, Color gold) {
    Pixmap pixmap = new Pixmap(FRAME_SIZE, FRAME_SIZE, Pixmap.Format.RGBA8888);
    pixmap.setColor(Color.valueOf("05060B"));
    pixmap.fill();

    pixmap.setColor(bronze);
    pixmap.fillRectangle(2, 2, FRAME_SIZE - 4, FRAME_SIZE - 4);
    pixmap.setColor(gold);
    pixmap.drawRectangle(3, 3, FRAME_SIZE - 7, FRAME_SIZE - 7);

    pixmap.setColor(Color.valueOf("332116"));
    pixmap.fillRectangle(6, 6, FRAME_SIZE - 12, FRAME_SIZE - 12);
    pixmap.setColor(face);
    pixmap.fillRectangle(7, 7, FRAME_SIZE - 14, FRAME_SIZE - 14);

    // Large square corner plates make the rivets survive scaling to the game's virtual viewport.
    int far = FRAME_SIZE - 9;
    drawCornerPlate(pixmap, 3, 3, bronze, gold);
    drawCornerPlate(pixmap, far, 3, bronze, gold);
    drawCornerPlate(pixmap, 3, far, bronze, gold);
    drawCornerPlate(pixmap, far, far, bronze, gold);

    Texture texture = new Texture(pixmap);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    pixmap.dispose();
    return new NinePatchDrawable(
        new NinePatch(texture, FRAME_SPLIT, FRAME_SPLIT, FRAME_SPLIT, FRAME_SPLIT));
  }

  private static void drawCornerPlate(Pixmap pixmap, int x, int y, Color bronze, Color gold) {
    pixmap.setColor(Color.valueOf("08080C"));
    pixmap.fillRectangle(x, y, 7, 7);
    pixmap.setColor(gold);
    pixmap.drawRectangle(x, y, 6, 6);
    pixmap.setColor(bronze);
    pixmap.fillRectangle(x + 2, y + 2, 3, 3);
    pixmap.setColor(Color.valueOf("FFE075"));
    pixmap.fillRectangle(x + 3, y + 3, 2, 2);
  }
}

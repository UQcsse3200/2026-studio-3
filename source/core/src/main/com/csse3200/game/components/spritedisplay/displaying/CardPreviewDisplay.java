package com.csse3200.game.components.spritedisplay.displaying;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.csse3200.game.components.battle.DeckEditorEvents;
import com.csse3200.game.ui.PopupDisplay;

public class CardPreviewDisplay extends Displaying {

  public record Content(String texturePath, String details) {
    public static final Content NONE = new Content(null, "");
  }

  private static final float ART_MAX_HEIGHT_FRACTION = 0.6f;
  private static final float ART_TEXT_GAP = 10f;

  // --- Styling constants ---
  private static final float BORDER_WIDTH = 4f;
  private static final float PADDING = 8f;
  private static final float BOTTOM_PADDING = 20f; // NEW: Space between panel and bottom of popup
  private static final Color BACKGROUND_COLOR = new Color(0.76f, 0.60f, 0.42f, 1f); // Light Brown
  private static final Color BORDER_COLOR = new Color(1f, 0.84f, 0f, 1f); // Gold

  private PopupDisplay popup;
  private final Image art = new Image();
  private final Map<String, Texture> textures = new HashMap<>();
  private float textureWidth = 1f;
  private float textureHeight = 1f;
  private boolean open;
  private boolean hasContent;

  private Image background;
  private Image borderTop;
  private Image borderBottom;
  private Image borderLeft;
  private Image borderRight;
  private Texture whitePixel;

  public CardPreviewDisplay(DisplayingRecord rec) {
    super(rec);
    art.setTouchable(Touchable.disabled);
    art.setVisible(false);
    label.setWrap(true);
    label.setAlignment(Align.topLeft);
    label.setTouchable(Touchable.disabled);
    label.setVisible(false);
  }

  public void addPopup(PopupDisplay popup) {
    this.popup = popup;
  }

  @Override
  public void create() {
    super.create();

    createSolidColorActors();

    stage.addActor(background);
    stage.addActor(borderTop);
    stage.addActor(borderBottom);
    stage.addActor(borderLeft);
    stage.addActor(borderRight);
    stage.addActor(art);

    entity.getEvents().addListener(DeckEditorEvents.OPENED, () -> setOpen(true));
    entity.getEvents().addListener(DeckEditorEvents.CLOSED, () -> setOpen(false));
    entity
        .getEvents()
        .addListener(
            DeckEditorEvents.TO_FRONT,
            () -> {
              background.toFront();
              borderTop.toFront();
              borderBottom.toFront();
              borderLeft.toFront();
              borderRight.toFront();
              art.toFront();
              label.toFront();
            });
  }

  private void createSolidColorActors() {
    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pixmap.setColor(Color.WHITE);
    pixmap.fill();
    whitePixel = new Texture(pixmap);
    pixmap.dispose();

    TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(whitePixel));

    background = new Image(drawable);
    background.setColor(BACKGROUND_COLOR);
    background.setTouchable(Touchable.disabled);

    borderTop = new Image(drawable);
    borderTop.setColor(BORDER_COLOR);
    borderTop.setTouchable(Touchable.disabled);

    borderBottom = new Image(drawable);
    borderBottom.setColor(BORDER_COLOR);
    borderBottom.setTouchable(Touchable.disabled);

    borderLeft = new Image(drawable);
    borderLeft.setColor(BORDER_COLOR);
    borderLeft.setTouchable(Touchable.disabled);

    borderRight = new Image(drawable);
    borderRight.setColor(BORDER_COLOR);
    borderRight.setTouchable(Touchable.disabled);
  }

  @Override
  public void onTrigger(Object payload) {
    if (!(payload instanceof Content content) || content.texturePath() == null) {
      hasContent = false;
      refreshVisibility();
      return;
    }
    Texture texture = loadTexture(content.texturePath());
    if (texture == null) {
      hasContent = false;
      refreshVisibility();
      return;
    }
    textureWidth = texture.getWidth();
    textureHeight = texture.getHeight();
    art.setDrawable(new TextureRegionDrawable(new TextureRegion(texture)));
    label.setText(content.details());
    hasContent = true;
    refreshVisibility();
  }

  private Texture loadTexture(String texturePath) {
    try {
      return textures.computeIfAbsent(texturePath, p -> new Texture(Gdx.files.internal(p)));
    } catch (GdxRuntimeException e) {
      Gdx.app.error("CardPreviewDisplay", "Could not load card art: " + texturePath, e);
      return null;
    }
  }

  private void setOpen(boolean open) {
    this.open = open;
    refreshVisibility();
  }

  private void refreshVisibility() {
    boolean show = open && hasContent;
    art.setVisible(show);
    label.setVisible(show);
    background.setVisible(show);
    borderTop.setVisible(show);
    borderBottom.setVisible(show);
    borderLeft.setVisible(show);
    borderRight.setVisible(show);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // We no longer check getHeight() here because we are calculating it dynamically
    if (!open || !hasContent || getWidth() <= 0) {
      return;
    }

    float panelX = popup.getWindowX() + getX();
    float panelTopY = popup.getWindowY() + popup.getWindowHeight() - getY();

    // --- NEW: Calculate bottom to reach the bottom of the popup ---
    float panelBottomY = popup.getWindowY() + BOTTOM_PADDING;

    // --- NEW: Calculate dynamic height based on the popup's bottom ---
    float panelHeight = panelTopY - panelBottomY;

    // Position and size background and borders using dynamic panelHeight
    background.setPosition(panelX, panelBottomY);
    background.setSize(getWidth(), panelHeight);

    borderTop.setPosition(panelX, panelTopY - BORDER_WIDTH);
    borderTop.setSize(getWidth(), BORDER_WIDTH);

    borderBottom.setPosition(panelX, panelBottomY);
    borderBottom.setSize(getWidth(), BORDER_WIDTH);

    borderLeft.setPosition(panelX, panelBottomY);
    borderLeft.setSize(BORDER_WIDTH, panelHeight);

    borderRight.setPosition(panelX + getWidth() - BORDER_WIDTH, panelBottomY);
    borderRight.setSize(BORDER_WIDTH, panelHeight);

    // Adjust content area to account for border and padding using dynamic panelHeight
    float contentX = panelX + BORDER_WIDTH + PADDING;
    float contentTopY = panelTopY - BORDER_WIDTH - PADDING;
    float contentWidth = getWidth() - 2 * (BORDER_WIDTH + PADDING);
    float contentHeight = panelHeight - 2 * (BORDER_WIDTH + PADDING);

    float scale =
        Math.min(
            contentWidth / textureWidth, (contentHeight * ART_MAX_HEIGHT_FRACTION) / textureHeight);
    float artWidth = textureWidth * scale;
    float artHeight = textureHeight * scale;

    art.setSize(artWidth, artHeight);
    art.setPosition(contentX + (contentWidth - artWidth) / 2f, contentTopY - artHeight);

    float textHeight = Math.max(0f, contentHeight - artHeight - ART_TEXT_GAP);
    label.setSize(contentWidth, textHeight);
    label.setPosition(contentX, contentTopY - artHeight - ART_TEXT_GAP - textHeight);
  }

  @Override
  public void dispose() {
    super.dispose();
    art.remove();
    if (background != null) background.remove();
    if (borderTop != null) borderTop.remove();
    if (borderBottom != null) borderBottom.remove();
    if (borderLeft != null) borderLeft.remove();
    if (borderRight != null) borderRight.remove();
    if (whitePixel != null) whitePixel.dispose();

    for (Texture texture : textures.values()) {
      texture.dispose();
    }
    textures.clear();
  }
}

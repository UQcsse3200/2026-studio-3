package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.Gdx;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Display-only preview of one card: its art, with a block of text (name, stats, ...) underneath.
 * Anchored to a {@link PopupDisplay}'s window like {@link PopupTextDisplay}: the record's {@code
 * x}/{@code y} are offsets from the window's top-left corner and {@code size} is the whole preview
 * panel. The art is scaled to fit the panel width keeping its aspect ratio, and the text sits
 * directly beneath whatever size the art ends up, so there's no dead space for landscape art.
 *
 * <p>Not interactive (no click/hover handling) — that's why this is a {@code Displaying} and not a
 * {@code Clickable}. Content is pushed in by firing the record's {@code trigger} with a {@link
 * Content} payload.
 */
public class CardPreviewDisplay extends Displaying {

  /**
   * @param texturePath internal path of the card art (null = nothing to show)
   * @param details text shown under the art
   */
  public record Content(String texturePath, String details) {
    /** Clears the preview. */
    public static final Content NONE = new Content(null, "");
  }

  private static final float ART_MAX_HEIGHT_FRACTION = 0.6f; // of the panel height
  private static final float ART_TEXT_GAP = 10f;

  private PopupDisplay popup;
  private final Image art = new Image();
  private final Map<String, Texture> textures = new HashMap<>();
  private float textureWidth = 1f;
  private float textureHeight = 1f;
  private boolean open;
  private boolean hasContent;

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
    stage.addActor(art);
    entity.getEvents().addListener(DeckEditorEvents.OPENED, () -> setOpen(true));
    entity.getEvents().addListener(DeckEditorEvents.CLOSED, () -> setOpen(false));
    entity
        .getEvents()
        .addListener(
            DeckEditorEvents.TO_FRONT,
            () -> {
              art.toFront();
              label.toFront();
            });
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

  /**
   * ASSUMPTION: a card's {@code texturePath} is a plain image file on the internal classpath (e.g.
   * "images/cards/strike.png"). If CardImageSkins loads card art some other way (atlas region,
   * ResourceService, ...), this is the only method to change.
   */
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
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (!open || !hasContent || getWidth() <= 0 || getHeight() <= 0) {
      return;
    }
    float panelX = popup.getWindowX() + getX();
    float panelTopY = popup.getWindowY() + popup.getWindowHeight() - getY();

    float scale =
        Math.min(
            getWidth() / textureWidth, (getHeight() * ART_MAX_HEIGHT_FRACTION) / textureHeight);
    float artWidth = textureWidth * scale;
    float artHeight = textureHeight * scale;
    art.setSize(artWidth, artHeight);
    art.setPosition(panelX + (getWidth() - artWidth) / 2f, panelTopY - artHeight);

    float textHeight = Math.max(0f, getHeight() - artHeight - ART_TEXT_GAP);
    label.setSize(getWidth(), textHeight);
    label.setPosition(panelX, panelTopY - artHeight - ART_TEXT_GAP - textHeight);
  }

  @Override
  public void dispose() {
    super.dispose();
    art.remove();
    for (Texture texture : textures.values()) {
      texture.dispose();
    }
    textures.clear();
  }
}

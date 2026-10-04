package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.battle.DeckEditorEvents;
import java.util.ArrayList;
import java.util.List;

public class CardFramesDisplay extends Displaying {

  public record Frame(String name, float x, float y, float width, float height) {}

  private static final Color FRAME_COLOR = new Color(0.82f, 0.71f, 0.55f, 1f); // Beige

  // A static white pixel to avoid skin issues
  private static Texture whitePixel;
  private static TextureRegionDrawable whiteDrawable;

  static {
    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pixmap.setColor(Color.WHITE);
    pixmap.fill();
    whitePixel = new Texture(pixmap);
    pixmap.dispose();
    whiteDrawable = new TextureRegionDrawable(new TextureRegion(whitePixel));
  }

  private final List<Image> frameBackgrounds = new ArrayList<>();
  private final List<Label> frameLabels = new ArrayList<>();

  public CardFramesDisplay(DisplayingRecord rec) {
    super(rec);
    label.setVisible(false);
  }

  @Override
  public void create() {
    super.create();
    // CHANGED: Now calls bringToFront() instead of bringToBack()
    entity.getEvents().addListener(DeckEditorEvents.TO_FRONT_BEHIND_CARDS, this::bringToFront);
    entity.getEvents().addListener(DeckEditorEvents.CLOSED, this::clear);
  }

  @Override
  public void onTrigger(Object payload) {
    clear();
    if (!(payload instanceof List<?> list)) {
      return;
    }

    for (Object item : list) {
      if (item instanceof Frame frame) {
        addFrame(frame);
      }
    }
  }

  private void addFrame(Frame frame) {
    // 1. The Beige Background
    Image bg = new Image(whiteDrawable);
    bg.setColor(FRAME_COLOR);
    bg.setBounds(frame.x(), frame.y(), frame.width(), frame.height());
    bg.setTouchable(Touchable.disabled);
    stage.addActor(bg);
    frameBackgrounds.add(bg);

    // 2. The Card Name (at the bottom)
    Label nameLabel = new Label(frame.name(), skin);
    nameLabel.setAlignment(Align.center);
    nameLabel.setColor(Color.BLACK);
    nameLabel.setTouchable(Touchable.disabled);
    nameLabel.setFontScale(0.5f);
    nameLabel.setWrap(true);

    // Position label at the bottom of the frame
    nameLabel.setBounds(frame.x(), frame.y() + 4f, frame.width(), 20f);

    stage.addActor(nameLabel);
    frameLabels.add(nameLabel);
  }

  // CHANGED: This used to be bringToBack()
  private void bringToFront() {
    for (Image bg : frameBackgrounds) bg.toFront();
    for (Label lbl : frameLabels) lbl.toFront();
  }

  private void clear() {
    for (Image bg : frameBackgrounds) bg.remove();
    for (Label lbl : frameLabels) lbl.remove();
    frameBackgrounds.clear();
    frameLabels.clear();
  }

  @Override
  public void dispose() {
    clear();
    super.dispose();
  }
}

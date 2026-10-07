package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

/** A ui component for displaying player's energy */
public class PlayerEnergyDisplay extends UIComponent {
  /** Live visual target; does not expose or change energy rules. */
  public com.badlogic.gdx.scenes.scene2d.Actor getEnergyActor() {
    return stack;
  }

  Stack stack;
  private com.badlogic.gdx.math.Rectangle opaquePixels;

  /** Bounds of visible icon pixels, excluding transparent texture padding. */
  public com.badlogic.gdx.math.Rectangle getEnergyVisualBounds() {
    energyImage.validate();
    if (opaquePixels == null) {
      com.badlogic.gdx.graphics.Pixmap source =
          new com.badlogic.gdx.graphics.Pixmap(
              com.badlogic.gdx.Gdx.files.internal("images/energy.png"));
      int left = source.getWidth(), top = source.getHeight(), right = 0, bottom = 0;
      for (int y = 0; y < source.getHeight(); y++)
        for (int x = 0; x < source.getWidth(); x++)
          if ((source.getPixel(x, y) & 255) > 16) {
            left = Math.min(left, x);
            top = Math.min(top, y);
            right = Math.max(right, x + 1);
            bottom = Math.max(bottom, y + 1);
          }
      opaquePixels =
          new com.badlogic.gdx.math.Rectangle(
              (float) left / source.getWidth(),
              1f - (float) bottom / source.getHeight(),
              (float) (right - left) / source.getWidth(),
              (float) (bottom - top) / source.getHeight());
      source.dispose();
    }
    com.badlogic.gdx.math.Vector2 origin =
        energyImage.localToStageCoordinates(
            new com.badlogic.gdx.math.Vector2(
                energyImage.getImageX() + opaquePixels.x * energyImage.getImageWidth(),
                energyImage.getImageY() + opaquePixels.y * energyImage.getImageHeight()));
    return new com.badlogic.gdx.math.Rectangle(
        origin.x - 3,
        origin.y - 3,
        opaquePixels.width * energyImage.getImageWidth() + 6,
        opaquePixels.height * energyImage.getImageHeight() + 6);
  }

  Table table;
  Label energyLabel;
  Image energyImage;
  private static final float FONT_SCALE = 0.75f;
  private static final String STYLE_NAME_LARGE = "large";

  /** Creates reusable ui styles and adds actors to the stage. */
  @Override
  public void create() {
    super.create();
    addActors();
    entity.getEvents().addListener("updateEnergy", this::updatePlayerEnergyUI);
  }

  /**
   * Creates actors and positions them on the stage using a table.
   *
   * @see Table for positioning options
   */
  private void addActors() {
    stack = new Stack();
    table = new Table(skin);

    // Energy image
    energyImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/energy.png", Texture.class));
    energyImage.setScaling(Scaling.fit);

    // Energy text
    EnergyComponent energyComponent = entity.getComponent(EnergyComponent.class);
    int currentEnergy = energyComponent.getCurrentEnergy();
    int maxEnergy = energyComponent.getMaxEnergy();
    CharSequence energyText = String.format("%d / %d", currentEnergy, maxEnergy);
    energyLabel = new Label(energyText, skin, STYLE_NAME_LARGE);
    energyLabel.setFontScale(FONT_SCALE);

    stack.add(energyImage);
    stack.add(energyLabel);
    energyLabel.setAlignment(Align.center);
    table.add(stack).size(400f);
    stage.addActor(table);
    table.setPosition(150f, 200f);
  }

  /**
   * Updates the player's energy on the ui.
   *
   * @param currentEnergy player's current energy
   * @param maxEnergy player's max energy
   */
  public void updatePlayerEnergyUI(int currentEnergy, int maxEnergy) {
    CharSequence text = String.format("%d / %d", currentEnergy, maxEnergy);
    energyLabel.setText(text);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // draw is handled by the stage
  }

  @Override
  public void dispose() {
    super.dispose();
    energyImage.remove();
    energyLabel.remove();
  }
}

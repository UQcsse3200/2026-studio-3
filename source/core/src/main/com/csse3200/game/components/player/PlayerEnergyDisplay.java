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
  Stack stack;
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

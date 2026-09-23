package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

/** A ui component for displaying player stats, e.g. health. */
public class PlayerStatsDisplay extends UIComponent {
  Table table;
  private Image heartImage;
  private Label healthLabel;
  private Image energyImage;
  private Label energyLabel;
  private static final float FONT_SCALE = 0.75f;
  private static final String STYLE_NAME_LARGE = "large";

  /** Creates reusable ui styles and adds actors to the stage. */
  @Override
  public void create() {
    super.create();
    addActors();

    entity.getEvents().addListener("updateHealth", this::updatePlayerHealthUI);
    entity.getEvents().addListener("updateEnergy", this::updatePlayerEnergyUI);
  }

  /**
   * Creates actors and positions them on the stage using a table.
   *
   * @see Table for positioning options
   */
  private void addActors() {
    table = new Table(skin);

    // Image size
    float imageSideLength = 20f;

    // Heart image
    heartImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/heart.png", Texture.class));

    // Health text
    int currentHealth = entity.getComponent(CombatStatsComponent.class).getHealth();
    int maxHealth = entity.getComponent(CombatStatsComponent.class).getMaxHealth();
    CharSequence healthText = String.format("Health: %d / %d", currentHealth, maxHealth);
    healthLabel = new Label(healthText, skin, STYLE_NAME_LARGE);
    healthLabel.setFontScale(FONT_SCALE);

    // Energy image
    energyImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/energy.png", Texture.class));

    // Energy text
    EnergyComponent energyComponent = entity.getComponent(EnergyComponent.class);
    int currentEnergy = energyComponent.getCurrentEnergy();
    int maxEnergy = energyComponent.getMaxEnergy();
    CharSequence energyText = String.format("Energy: %d / %d", currentEnergy, maxEnergy);
    energyLabel = new Label(energyText, skin, STYLE_NAME_LARGE);
    energyLabel.setFontScale(FONT_SCALE);

    table.add(heartImage).size(imageSideLength).pad(5);
    table.add(healthLabel);
    table.row();

    table.add(energyImage).size(imageSideLength).pad(5);
    table.add(energyLabel).left();
    stage.addActor(table);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // draw is handled by the stage
  }

  @Override
  public void update() {
    updatePosition();
  }

  /** Updates the position of the enemy's stats, so they are displayed directly below the enemy */
  public void updatePosition() {
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();

    float enemyX = position.x + scale.x / 2f;
    float enemyY = position.y - 0.5f;

    Vector3 screenPosition = new Vector3(enemyX, enemyY, 0);

    Camera camera = ServiceLocator.getCamera();
    if (camera == null) {
      return;
    }
    camera.project(screenPosition); // converts coordinates

    table.setPosition(screenPosition.x - table.getWidth() / 2f, screenPosition.y);
  }

  /**
   * Updates the player's health on the ui.
   *
   * @param currentHealth player's current health
   * @param maxHealth player's max health
   */
  public void updatePlayerHealthUI(int currentHealth, int maxHealth) {
    CharSequence text = String.format("Health: %d / %d", currentHealth, maxHealth);
    healthLabel.setText(text);
  }

  /**
   * Updates the player's energy on the ui.
   *
   * @param currentEnergy player's current energy
   * @param maxEnergy player's max energy
   */
  public void updatePlayerEnergyUI(int currentEnergy, int maxEnergy) {
    CharSequence text = String.format("Energy: %d / %d", currentEnergy, maxEnergy);
    energyLabel.setText(text);
  }

  @Override
  public void dispose() {
    super.dispose();
    heartImage.remove();
    healthLabel.remove();
    energyImage.remove();
    energyLabel.remove();
  }
}

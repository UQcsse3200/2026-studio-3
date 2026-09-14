package com.csse3200.game.components.enemy;

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

/** A UI component for displaying enemy stats */
public class EnemyStatsDisplay extends UIComponent {
  Table table;
  private Image heartImage;
  private Label healthLabel;
  private static final float FONT_SCALE = 0.75f;
  private static final String STYLE_NAME_LARGE = "large";

  public void create() {
    super.create();
    addActors();

    entity.getEvents().addListener("updateHealth", this::updateEnemyHealthUI);
  }

  /**
   * Creates actors and positions them on the stage using a table.
   *
   * @see Table for positioning options
   */
  private void addActors() {
    table = new Table(skin);
    // table.setPosition(ENEMY_SPAWN.x, ENEMY_SPAWN.y);
    // table.setFillParent(true);
    // table.padTop(45f).padLeft(5f);

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

    table.add(heartImage).size(imageSideLength).pad(5);
    table.add(healthLabel);
    table.pack();
    stage.addActor(table);
    updatePosition();
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
    ServiceLocator.getCamera().project(screenPosition); // converts coordinates

    table.setPosition(screenPosition.x - table.getWidth() / 2f, screenPosition.y);
  }

  /**
   * Updates the enemy's health on the ui.
   *
   * @param currentHealth enemy's current health
   * @param maxHealth enemy's max health
   */
  public void updateEnemyHealthUI(int currentHealth, int maxHealth) {
    CharSequence text = String.format("Health: %d / %d", currentHealth, maxHealth);
    healthLabel.setText(text);
  }

  @Override
  public void dispose() {
    super.dispose();
    heartImage.remove();
    healthLabel.remove();
  }
}

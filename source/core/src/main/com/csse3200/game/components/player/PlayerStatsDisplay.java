package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

/** A ui component for displaying player stats, e.g. health. */
public class PlayerStatsDisplay extends UIComponent {
  Table table;
  private Image heartImage;
  private ProgressBar healthBar;
  private Label healthLabel;
  private Stack stack;
  private static final float FONT_SCALE = 1f;
  private static final String STYLE_NAME_WHITE = "white";
  private static final String STYLE_NAME_LARGE = "large";

  /** Creates reusable ui styles and adds actors to the stage. */
  @Override
  public void create() {
    super.create();
    addActors();

    entity.getEvents().addListener("updateHealth", this::updatePlayerHealthUI);
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

    // Health label
    int currentHealth = entity.getComponent(CombatStatsComponent.class).getHealth();
    int maxHealth = entity.getComponent(CombatStatsComponent.class).getMaxHealth();
    CharSequence healthText = String.format("%d / %d", currentHealth, maxHealth);
    healthLabel = new Label(healthText, skin);
    healthLabel.setColor(Color.WHITE);
    healthLabel.setFontScale(FONT_SCALE);
    healthLabel.setAlignment(Align.center);

    // Health bar
    ProgressBar.ProgressBarStyle healthBarStyle = new ProgressBar.ProgressBarStyle();
    healthBarStyle.background = skin.newDrawable(STYLE_NAME_WHITE, Color.DARK_GRAY);
    healthBarStyle.knobBefore = skin.newDrawable(STYLE_NAME_WHITE, Color.GREEN);
    healthBarStyle.background.setMinHeight(20);
    healthBarStyle.knobBefore.setMinHeight(20);

    healthBar = new ProgressBar(0, maxHealth, 1, false, healthBarStyle);
    healthBar.setSize(150, 20);
    healthBar.setValue(currentHealth);
    healthBar.setAnimateDuration(0.2f);

    // Stack
    stack = new Stack();
    stack.add(healthBar);
    stack.add(healthLabel);

    table.add(heartImage).size(imageSideLength).pad(5);
    table.add(stack).width(150).height(30).pad(5).left();

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

  /** Updates the position of the player's stats, so they are displayed directly below the player */
  public void updatePosition() {
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();

    float enemyX = position.x + scale.x / 2f;
    float enemyY = position.y - 1.25f;

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
    CharSequence text = String.format("%d / %d", currentHealth, maxHealth);
    healthLabel.setText(text);
    healthBar.setRange(0, maxHealth);
    healthBar.setValue(currentHealth);

    if ((float) currentHealth / maxHealth <= 0.4f) {
      healthBar.getStyle().knobBefore = skin.newDrawable(STYLE_NAME_WHITE, Color.RED);
      healthBar.getStyle().background.setMinHeight(20);
      healthBar.getStyle().knobBefore.setMinHeight(20);
    } else {
      healthBar.getStyle().knobBefore = skin.newDrawable(STYLE_NAME_WHITE, Color.GREEN);
      healthBar.getStyle().background.setMinHeight(20);
      healthBar.getStyle().knobBefore.setMinHeight(20);
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    heartImage.remove();
    healthBar.remove();
  }
}

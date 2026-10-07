package com.csse3200.game.components.enemy;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

/** A UI component for displaying enemy stats */
public class EnemyStatsDisplay extends UIComponent {
  Table table;
  private Label healthLabel;
  private ProgressBar healthBar;
  private Stack healthStack;
  private Stack armourStack;
  private Label armourLabel;
  private Cell<Stack> armourCell;
  private static final float FONT_SCALE = 1f;
  private static final String STYLE_NAME_WHITE = "white";

  @Override
  public void create() {
    super.create();
    addActors();

    entity.getEvents().addListener("updateHealth", this::updateEnemyHealthUI);
    entity.getEvents().addListener("updateArmour", this::updateEnemyArmourUI);
  }

  /**
   * Creates actors and positions them on the stage using a table.
   *
   * @see Table for positioning options
   */
  private void addActors() {
    table = new Table(skin);

    // Armour image
    Image armourImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/armour.png", Texture.class));

    // Armour text
    int armour = entity.getComponent(CombatStatsComponent.class).getArmour();
    CharSequence armourText = String.format("%d", armour);
    armourLabel = new Label(armourText, skin);
    armourLabel.setColor(Color.WHITE);
    armourLabel.setFontScale(FONT_SCALE);
    armourLabel.setAlignment(Align.center);

    // Armour stack
    armourStack = new Stack();
    armourStack.add(armourImage);
    armourStack.add(armourLabel);

    // Health text
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

    if (armour > 0) {
      healthBarStyle.knobBefore = skin.newDrawable(STYLE_NAME_WHITE, Color.SKY);
    } else {
      healthBarStyle.knobBefore = skin.newDrawable(STYLE_NAME_WHITE, Color.GREEN);
    }
    healthBarStyle.background.setMinHeight(20);
    healthBarStyle.knobBefore.setMinHeight(20);

    healthBar = new ProgressBar(0, (float) maxHealth, 1, false, healthBarStyle);
    healthBar.setSize(150, 20);
    healthBar.setValue((float) currentHealth);
    healthBar.setAnimateDuration(0.2f);

    // Health stack
    healthStack = new Stack();
    healthStack.add(healthBar);
    healthStack.add(healthLabel);

    // Add stacks and cell to table
    armourCell = table.add(armourStack).size(60f);
    updateArmourVisibility(armour);
    table.add(healthStack).width(150).height(30);

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
    float enemyY = position.y - 0.6f;

    Vector3 screenPosition = new Vector3(enemyX, enemyY, 0);

    Camera camera = ServiceLocator.getCamera();
    if (camera == null) {
      return;
    }
    camera.project(screenPosition); // converts coordinates

    table.setPosition(screenPosition.x - table.getWidth() / 2f, screenPosition.y);
  }

  /**
   * Updates the enemy's health on the ui.
   *
   * @param currentHealth enemy's current health
   * @param maxHealth enemy's max health
   */
  public void updateEnemyHealthUI(int currentHealth, int maxHealth) {
    CharSequence text = String.format("%d / %d", currentHealth, maxHealth);
    healthLabel.setText(text);
    healthBar.setRange(0, (float) maxHealth);
    healthBar.setValue((float) currentHealth);
    updateHealthBarColour();
  }

  /**
   * Updates the enemy's armour on the UI
   *
   * @param armour the enemy's armour
   */
  public void updateEnemyArmourUI(int armour) {
    CharSequence text = String.format("%d", armour);
    armourLabel.setText(text);
    updateArmourVisibility(armour);
    updateHealthBarColour();
  }

  /**
   * Updates the colour of the health bar. If the entity has armour the colour turns blue. If the
   * entity's health reaches 40% the colour turns red.
   */
  public void updateHealthBarColour() {
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    int currentHealth = stats.getHealth();
    int maxHealth = stats.getMaxHealth();
    int armour = stats.getArmour();

    if (armour > 0) {
      healthBar.getStyle().knobBefore = skin.newDrawable(STYLE_NAME_WHITE, Color.SKY);
      healthBar.getStyle().knobBefore.setMinHeight(20);
    } else if ((float) currentHealth / maxHealth <= 0.4f) {
      healthBar.getStyle().knobBefore = skin.newDrawable(STYLE_NAME_WHITE, Color.RED);
      healthBar.getStyle().knobBefore.setMinHeight(20);
    } else {
      healthBar.getStyle().knobBefore = skin.newDrawable(STYLE_NAME_WHITE, Color.GREEN);
      healthBar.getStyle().knobBefore.setMinHeight(20);
    }
  }

  /**
   * Makes the armour cell visible if entity has armour
   *
   * @param armour the amount of armour the entity has
   */
  public void updateArmourVisibility(int armour) {
    if (armour > 0) {
      armourCell.setActor(armourStack);
      armourCell.size(60f);
    } else {
      armourCell.setActor(null);
      armourCell.size(0f);
    }
    table.pack();
  }

  @Override
  public void dispose() {
    super.dispose();
    healthStack.remove();
    armourStack.remove();
  }
}

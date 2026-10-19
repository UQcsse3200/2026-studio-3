package com.csse3200.game.components;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

public class StatsDisplay extends UIComponent {

  private static final String STYLE_NAME_WHITE = "white";
  private static final float FONT_SCALE = 1f;
  private static final float HEALTH_BAR_HEIGHT = 20f;
  private static final float HEALTH_BAR_WIDTH = 150f;
  private static final float CELL_SIZE = 60f;

  Table table;
  private ProgressBar healthBar;
  private Label healthLabel;

  private Stack armourStack;
  private Label armourLabel;
  private Cell<Stack> armourCell;

  private Label blockLabel;
  private Stack blockStack;
  private Cell<Stack> blockCell;

  private boolean disposed;

  @Override
  public void create() {
    super.create();
    addActors();
    entity.getEvents().addListener("updateHealth", this::updatePlayerHealthUI);
    entity.getEvents().addListener("updateArmour", this::updateArmourUI);
    entity.getEvents().addListener("updateBlock", this::updateEnemyBlockUI);
  }

  private void addActors() {
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    table = new Table();

    // Armour display
    Image armourImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/armour.png", Texture.class));

    armourLabel = createLabel(String.valueOf(stats.getArmour()));

    armourStack = new Stack();
    armourStack.add(armourImage);
    armourStack.add(armourLabel);

    armourCell = table.add(armourStack).size(CELL_SIZE);
    updateArmourVisibility(stats.getArmour());

    // Block display
    Image blockImage =
        new Image(
            ServiceLocator.getResourceService()
                .getAsset("images/effects/shield.png", Texture.class));

    blockLabel = createLabel(String.valueOf(stats.getBlock()));

    blockStack = new Stack();
    blockStack.add(blockImage);
    blockStack.add(blockLabel);

    blockCell = table.add(blockStack).size(CELL_SIZE);
    updateBlockVisibility(stats.getBlock());

    // Health display
    healthLabel = createLabel(String.format("%d / %d", stats.getHealth(), stats.getMaxHealth()));

    ProgressBar.ProgressBarStyle healthBarStyle = new ProgressBar.ProgressBarStyle();
    healthBarStyle.background = skin.newDrawable(STYLE_NAME_WHITE, Color.DARK_GRAY);

    if (stats.getArmour() > 0) {
      healthBarStyle.knobBefore = skin.newDrawable(STYLE_NAME_WHITE, Color.SKY);
    } else if (stats.getBlock() > 0) {
      healthBarStyle.knobBefore = skin.newDrawable(STYLE_NAME_WHITE, Color.VIOLET);
    } else {
      healthBarStyle.knobBefore = skin.newDrawable(STYLE_NAME_WHITE, Color.GREEN);
    }

    healthBarStyle.background.setMinHeight(HEALTH_BAR_HEIGHT);
    healthBarStyle.knobBefore.setMinHeight(HEALTH_BAR_HEIGHT);

    healthBar = new ProgressBar(0, (float) stats.getMaxHealth(), 1, false, healthBarStyle);
    healthBar.setSize(HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);
    healthBar.setValue((float) stats.getHealth());
    healthBar.setAnimateDuration(0.2f);

    Stack healthStack = new Stack();
    healthStack.add(healthBar);
    healthStack.add(healthLabel);

    table.add(healthStack).width(HEALTH_BAR_WIDTH).height(30);

    table.pack();

    stage.addActor(table);
    updatePosition();
  }

  private Label createLabel(String text) {
    Label label = new Label(text, skin);
    label.setColor(Color.WHITE);
    label.setFontScale(FONT_SCALE);
    label.setAlignment(Align.center);
    return label;
  }

  @Override
  public void draw(SpriteBatch batch) {
    // draw is handled by the stage
  }

  @Override
  public void update() {
    if (disposed) {
      return;
    }
    updatePosition();
  }

  /** Updates the position of the entity's stats, so they are displayed directly below the entity */
  public void updatePosition() {
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();

    float playerX = position.x + scale.x / 2f;
    float playerY = position.y - 0.6f;

    Vector3 screenPosition = new Vector3(playerX, playerY, 0);

    Camera camera = ServiceLocator.getCamera();
    if (camera == null) {
      return;
    }
    camera.project(screenPosition); // converts coordinates

    table.setPosition(screenPosition.x - table.getWidth() / 2f, screenPosition.y);
  }

  /**
   * Updates the entity's health on the ui.
   *
   * @param currentHealth entity's current health
   * @param maxHealth entity's max health
   */
  public void updatePlayerHealthUI(int currentHealth, int maxHealth) {
    if (healthLabel == null) {
      return;
    }
    CharSequence text = String.format("%d / %d", currentHealth, maxHealth);
    healthLabel.setText(text);
    healthBar.setRange(0, (float) maxHealth);
    healthBar.setValue((float) currentHealth);
    updateHealthBarColour();
  }

  /**
   * Updates the entity's armour on the UI
   *
   * @param armour the entity's armour
   */
  public void updateArmourUI(int armour) {
    if (armourLabel == null) {
      return;
    }
    CharSequence text = String.format("%d", armour);
    armourLabel.setText(text);
    updateArmourVisibility(armour);
    updateHealthBarColour();
  }

  /**
   * Updates the entity's block on the UI
   *
   * @param block the entity's block
   */
  public void updateEnemyBlockUI(int block) {
    if (blockLabel == null) {
      return;
    }
    CharSequence text = String.format("%d", block);
    blockLabel.setText(text);
    updateBlockVisibility(block);
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
    int block = stats.getBlock();

    Color colour;
    if (armour > 0) {
      colour = Color.SKY;
    } else if (block > 0) {
      colour = Color.VIOLET;
    } else if ((float) currentHealth / maxHealth <= 0.4f) {
      colour = Color.RED;
    } else {
      colour = Color.GREEN;
    }
    healthBar.getStyle().knobBefore = skin.newDrawable(STYLE_NAME_WHITE, colour);
    healthBar.getStyle().knobBefore.setMinHeight(HEALTH_BAR_HEIGHT);
  }

  /**
   * Makes the armour cell visible if entity has armour
   *
   * @param armour the amount of armour the entity has
   */
  public void updateArmourVisibility(int armour) {
    if (armourCell == null) {
      return;
    }

    if (armour > 0) {
      armourCell.setActor(armourStack);
      armourCell.size(CELL_SIZE);
    } else {
      armourCell.setActor(null);
      armourCell.size(0f);
    }
    table.pack();
  }

  /**
   * Makes the block cell visible if entity has block
   *
   * @param block the amount of block the entity has
   */
  public void updateBlockVisibility(int block) {
    if (blockCell == null) {
      return;
    }
    int armour = entity.getComponent(CombatStatsComponent.class).getArmour();
    if (block > 0 && armour <= 0) {
      blockCell.setActor(blockStack);
      blockCell.size(CELL_SIZE);
    } else {
      blockCell.setActor(null);
      blockCell.size(0f);
    }
  }

  /** Read-only live health display for tutorial anchoring and resolution observation. */
  public Label getHealthLabel() {
    return healthLabel;
  }

  public Label getArmourLabel() {
    return armourLabel;
  }

  public Label getBlockLabel() {
    return blockLabel;
  }

  @Override
  public void dispose() {
    if (disposed) {
      return;
    }
    disposed = true;
    super.dispose();
  }
}

package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

public class PlayerStatsTopDisplay extends UIComponent {
  Table table;
  Table levelTable;
  private Image playerImage;
  private Image heartImage;
  private Label healthLabel;
  private Image levelImage;
  private Label levelLabel;
  private Image moneyImage;
  private Label moneyLabel;
  private final RunState runState;
  private static final float FONT_SCALE = 0.75f;
  private static final String STYLE_NAME_LARGE = "large";

  public PlayerStatsTopDisplay(RunState runState) {
    this.runState = runState;
  }

  /** Creates reusable ui styles and adds actors to the stage. */
  @Override
  public void create() {
    super.create();
    addActors();

    entity.getEvents().addListener("updateHealth", this::updatePlayerHealthUI);
    entity.getEvents().addListener("updateLevel", this::updatePlayerLevelUI);
    entity.getEvents().addListener("updateMoney", this::updatePlayerMoneyUI);
  }

  /**
   * Creates actors and positions them on the stage using a table.
   *
   * @see Table for positioning options
   */
  private void addActors() {
    table = new Table(skin);
    table.top().left();
    table.setBackground(skin.newDrawable("color", new Color(0.105f, 0.070f, 0.065f, 0.98f)));
    table.padLeft(90f);

    // Image size
    float imageSideLength = 20f;
    float playerImageSize = imageSideLength + 50;

    TextureAtlas atlas =
        ServiceLocator.getResourceService()
            .getAsset("images/player/player.atlas", TextureAtlas.class);

    playerImage = new Image(atlas.findRegions("idle").first());
    playerImage.setSize(playerImageSize, playerImageSize);
    playerImage.setPosition(10f, stageHeight - playerImageSize + 30);

    // Heart image
    heartImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/heart.png", Texture.class));

    // Health text
    int currentHealth = entity.getComponent(CombatStatsComponent.class).getHealth();
    int maxHealth = entity.getComponent(CombatStatsComponent.class).getMaxHealth();
    CharSequence healthText = String.format("%d / %d", currentHealth, maxHealth);
    healthLabel = new Label(healthText, skin, STYLE_NAME_LARGE);
    healthLabel.setFontScale(FONT_SCALE);

    // Money image
    moneyImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/money.png", Texture.class));

    // Money text
    InventoryComponent inventoryComponent = entity.getComponent(InventoryComponent.class);
    int money = inventoryComponent.getGold();
    CharSequence moneyText = String.format("$%d", money);
    moneyLabel = new Label(moneyText, skin, STYLE_NAME_LARGE);
    moneyLabel.setFontScale(FONT_SCALE);

    // level table
    levelTable = new Table(skin);

    // Level image
    levelImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/level.png", Texture.class));

    // Level text
    String levelText = String.format("%d", runState.getMapProgression());
    levelLabel = new Label(levelText, skin, STYLE_NAME_LARGE);
    levelLabel.setFontScale(FONT_SCALE);

    // add stats to the gold and health table
    table.add(heartImage).size(imageSideLength).pad(5).center();
    table.add(healthLabel).left().pad(10).center();

    table.add(moneyImage).size(imageSideLength).pad(5).center();
    table.add(moneyLabel).left().pad(10).center();
    stage.addActor(table);

    // add stats to level table
    levelTable.add(levelImage).size(imageSideLength).pad(5);
    levelTable.add(levelLabel).left().pad(10);
    levelTable.pack();
    positionTopDisplay();
    stage.addActor(levelTable);
    stage.addActor(playerImage);
  }

 /**
  * Positions the top display based on the stage wdith and stage height.
  * Function only for positioning allows it to be called on update(), if resize
  * Occurs this will adjust the position and width of the top display 
  */
  private void positionTopDisplay() {
    float stageWidth = 
    stage.getViewport() == null ? Gdx.graphics.getWidth()
     : stage.getViewport().getWorldWidth();
    float stageHeight = stage.getViewport() == null ? Gdx.graphics.getHeight()
     : stage.getViewport().getWorldHeight();
    table.setSize(stageWidth, 75);
    table.setPosition(0, stageHeight - 75);
    levelTable.setPosition((stageWidth - levelTable.getWidth()) / 2f, stageHeight - 45);
  }

  @Override
  public void update() {
    positionTopDisplay();
  }

  @Override
  public void draw(SpriteBatch batch) {
    // draw is handled by the stage
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
  }

  /**
   * s* Updates the player's level on the ui.
   *
   * @param level player's current level
   */
  public void updatePlayerLevelUI(int level) {
    CharSequence text = String.format("%d", level);
    levelLabel.setText(text);
  }

  /**
   * Updates the player's money on the ui.
   *
   * @param money player money
   */
  public void updatePlayerMoneyUI(int money) {
    CharSequence text = String.format("$%d", money);
    moneyLabel.setText(text);
  }

  @Override
  public void dispose() {
    super.dispose();

    table.remove();
    levelTable.remove();
    playerImage.remove();
    heartImage.remove();
    healthLabel.remove();
    levelImage.remove();
    levelLabel.remove();
    moneyImage.remove();
    moneyLabel.remove();
  }
}

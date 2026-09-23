package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

public class PlayerStatsTopDisplay extends UIComponent {
  Table table;
  private Image levelImage;
  private Label levelLabel;
  private Image moneyImage;
  private Label moneyLabel;
  private RunState runState;
  private static final float FONT_SCALE = 0.75f;
  private static final String STYLE_NAME_LARGE = "large";
  private final float mapWidth = Gdx.graphics.getWidth();
  private final float mapHeight = Gdx.graphics.getHeight();

  public PlayerStatsTopDisplay(RunState runState) {
    this.runState = runState;
  }

  /** Creates reusable ui styles and adds actors to the stage. */
  @Override
  public void create() {
    super.create();
    addActors();

    entity.getEvents().addListener("updateLevel", this::updatePlayerLevelUI);
    entity.getEvents().addListener("updateMoney", this::updatePlayerMoneyUI);
  }

  /**
   * Creates actors and positions them on the stage using a table.
   *
   * @see Table for positioning options
   */
  private void addActors() {
    table = new Table();
    table.top().left();
    table.setSize(mapWidth, 50);
    table.setPosition(0, mapHeight - 50);
    table.setBackground(skin.newDrawable("color", new Color(0.105f, 0.070f, 0.065f, 0.98f)));
    // table.setFillParent(true);
    table.padTop(5f).padLeft(10f);

    // Image size
    float imageSideLength = 20f;

    // Level image
    levelImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/level.png", Texture.class));

    // Level text
    String levelText = String.format("Level: %d", runState.getMapProgression());
    levelLabel = new Label(levelText, skin, STYLE_NAME_LARGE);
    levelLabel.setFontScale(FONT_SCALE);

    // Money image
    moneyImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/money.png", Texture.class));

    // Money text
    InventoryComponent inventoryComponent = entity.getComponent(InventoryComponent.class);
    int money = inventoryComponent.getGold();
    CharSequence moneyText = String.format("Gold: $%d", money);
    moneyLabel = new Label(moneyText, skin, STYLE_NAME_LARGE);
    moneyLabel.setFontScale(FONT_SCALE);

    table.add(levelImage).size(imageSideLength).pad(5);
    table.add(levelLabel).left().pad(10);

    table.add(moneyImage).size(imageSideLength).pad(5);
    table.add(moneyLabel).left();
    stage.addActor(table);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // draw is handled by the stage
  }

  /**
   * s* Updates the player's level on the ui.
   *
   * @param level player's current level
   */
  public void updatePlayerLevelUI(int level) {
    CharSequence text = String.format("Level: %d", level);
    levelLabel.setText(text);
  }

  /**
   * Updates the player's money on the ui.
   *
   * @param money player money
   */
  public void updatePlayerMoneyUI(int money) {
    CharSequence text = String.format("Gold: $%d", money);
    moneyLabel.setText(text);
  }

  @Override
  public void dispose() {
    super.dispose();
    levelImage.remove();
    levelLabel.remove();
    moneyImage.remove();
    moneyLabel.remove();
  }
}

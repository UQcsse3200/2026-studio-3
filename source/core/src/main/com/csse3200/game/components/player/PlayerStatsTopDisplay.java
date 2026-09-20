package com.csse3200.game.components.player;

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
  private Image pietyImage;
  private Label pietyLabel;
  private Image moneyImage;
  private Label moneyLabel;
  private RunState runState;
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

    entity.getEvents().addListener("updatePiety", this::updatePlayerPietyUI);
    entity.getEvents().addListener("updateMoney", this::updatePlayerMoneyUI);
  }

  /**
   * Creates actors and positions them on the stage using a table.
   *
   * @see Table for positioning options
   */
  private void addActors() {
    table = new Table();
    table.top();
    table.setFillParent(true);
    table.padTop(45f).padLeft(5f);

    // Image size
    float imageSideLength = 20f;

    // Piety image
    pietyImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/piety.png", Texture.class));

    // Piety text
    String pietyText = String.format("Level: %d", runState.getMapProgression());
    pietyLabel = new Label(pietyText, skin, STYLE_NAME_LARGE);
    pietyLabel.setFontScale(FONT_SCALE);

    // Money image
    moneyImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/money.png", Texture.class));

    // Money text
    InventoryComponent inventoryComponent = entity.getComponent(InventoryComponent.class);
    int money = inventoryComponent.getGold();
    CharSequence moneyText = String.format("Gold: $%d", money);
    moneyLabel = new Label(moneyText, skin, STYLE_NAME_LARGE);
    moneyLabel.setFontScale(FONT_SCALE);

    table.add(pietyImage).size(imageSideLength).pad(5);
    table.add(pietyLabel).left().pad(10);

    table.add(moneyImage).size(imageSideLength).pad(5);
    table.add(moneyLabel).left();
    stage.addActor(table);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // draw is handled by the stage
  }

  /**
   * s* Updates the player's piety on the ui.
   *
   * @param piety player piety
   */
  public void updatePlayerPietyUI(int piety) {
    CharSequence text = String.format("Level: %d", piety);
    pietyLabel.setText(text);
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
    pietyImage.remove();
    pietyLabel.remove();
    moneyImage.remove();
    moneyLabel.remove();
  }
}

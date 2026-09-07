package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
  private Image pietyImage;
  private Label pietyLabel;
  private Image moneyImage;
  private Label moneyLabel;
  private static final float FONT_SCALE = 0.75f;

  /** Creates reusable ui styles and adds actors to the stage. */
  @Override
  public void create() {
    super.create();
    addActors();

    entity.getEvents().addListener("updateHealth", this::updatePlayerHealthUI);
    entity.getEvents().addListener("updateEnergy", this::updatePlayerEnergyUI);
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
    table.left();
    table.setFillParent(true);
    table.padTop(45f).padLeft(5f);

    // Image size
    float imageSideLength = 20f;

    // Heart image
    heartImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/heart.png", Texture.class));

    // Health text
    int currentHealth = entity.getComponent(CombatStatsComponent.class).getHealth();
    int maxHealth = entity.getComponent(CombatStatsComponent.class).getMaxHealth();
    CharSequence healthText = String.format("Health: %d / %d", currentHealth, maxHealth);
    healthLabel = new Label(healthText, skin, "large");
    healthLabel.setFontScale(FONT_SCALE);

    // Energy image
    energyImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/energy.png", Texture.class));

    // Energy text
    EnergyComponent energyComponent = entity.getComponent(EnergyComponent.class);
    int currentEnergy = energyComponent.getCurrentEnergy();
    int maxEnergy = energyComponent.getMaxEnergy();
    CharSequence energyText = String.format("Energy: %d / %d", currentEnergy, maxEnergy);
    energyLabel = new Label(energyText, skin, "large");
    energyLabel.setFontScale(FONT_SCALE);

    // Piety image
    pietyImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/piety.png", Texture.class));

    // Piety text
    pietyLabel = new Label("Level: 1", skin, "large");
    pietyLabel.setFontScale(FONT_SCALE);

    // Money image
    moneyImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/money.png", Texture.class));

    // Money text
    InventoryComponent inventoryComponent = entity.getComponent(InventoryComponent.class);
    int money = inventoryComponent.getGold();
    CharSequence moneyText = String.format("Gold: $%d", money);
    moneyLabel = new Label(moneyText, skin, "large");
    moneyLabel.setFontScale(FONT_SCALE);

    table.add(heartImage).size(imageSideLength).pad(5);
    table.add(healthLabel);
    table.row();

    table.add(energyImage).size(imageSideLength).pad(5);
    table.add(energyLabel).left();
    table.row();

    table.add(pietyImage).size(imageSideLength).pad(5);
    table.add(pietyLabel).left();
    table.row();

    table.add(moneyImage).size(imageSideLength).pad(5);
    table.add(moneyLabel).left();
    stage.addActor(table);
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

  /**
   * s* Updates the player's piety on the ui.
   *
   * @param piety player piety
   */
  public void updatePlayerPietyUI(int piety) {
    CharSequence text = String.format("Piety: %d", piety);
    pietyLabel.setText(text);
  }

  /**
   * Updates the player's money on the ui.
   *
   * @param money player money
   */
  public void updatePlayerMoneyUI(int money) {
    CharSequence text = String.format("Money: %d", money);
    moneyLabel.setText(text);
  }

  @Override
  public void dispose() {
    super.dispose();
    heartImage.remove();
    healthLabel.remove();
    energyImage.remove();
    energyLabel.remove();
    pietyImage.remove();
    pietyLabel.remove();
    moneyImage.remove();
    moneyLabel.remove();
  }
}

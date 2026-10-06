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
import com.csse3200.game.components.StatusEffect;
import com.csse3200.game.components.enemy.IntentIcons;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import java.util.HashMap;
import java.util.Map;

/** A ui component for displaying player stats, e.g. health. */
public class PlayerStatsDisplay extends UIComponent {
  Table table;
  private Label healthLabel;
  private Label energyLabel;
  private Image statusImage;
  private Table statusRow;
  private Table statusIcons;
  private Map<String, Integer> displayedStatusDurations;
  private boolean statusRowDirty = true;
  private boolean disposed;
  private final EventListener1<String> statusChangeListener = this::onStatusChanged;
  private static final float FONT_SCALE = 0.75f;
  private static final String STYLE_NAME_LARGE = "large";

  /** Creates reusable ui styles and adds actors to the stage. */
  @Override
  public void create() {
    super.create();
    addActors();

    entity.getEvents().addListener("updateHealth", this::updatePlayerHealthUI);
    entity.getEvents().addListener("updateEnergy", this::updatePlayerEnergyUI);
    entity.getEvents().addListener("statusEffectApplied", statusChangeListener);
    entity.getEvents().addListener("statusEffectRemoved", statusChangeListener);
    updateStatusRow();
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
    Image heartImage =
        new Image(ServiceLocator.getResourceService().getAsset("images/heart.png", Texture.class));

    // Health text
    int currentHealth = entity.getComponent(CombatStatsComponent.class).getHealth();
    int maxHealth = entity.getComponent(CombatStatsComponent.class).getMaxHealth();
    CharSequence healthText = String.format("Health: %d / %d", currentHealth, maxHealth);
    healthLabel = new Label(healthText, skin, STYLE_NAME_LARGE);
    healthLabel.setFontScale(FONT_SCALE);

    // Energy image
    Image energyImage =
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
    table.row();

    // Status effects: hidden until something is active, then one icon and count per effect.
    statusImage =
        new Image(ServiceLocator.getResourceService().getAsset(IntentIcons.DEBUFF, Texture.class));

    Label statusLabel = new Label("Debuff:", skin, STYLE_NAME_LARGE);
    statusLabel.setFontScale(FONT_SCALE);

    statusIcons = new Table(skin);
    statusRow = new Table(skin);
    statusRow.add(statusLabel).padRight(8f);
    statusRow.add(statusIcons).left();

    table.add(statusImage).size(imageSideLength).pad(5);
    table.add(statusRow).left();

    stage.addActor(table);
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
    updateStatusRow();
  }

  private void onStatusChanged(String statusKey) {
    if (isKnownDebuff(statusKey)) {
      statusRowDirty = true;
    }
  }

  private boolean hasStatusDurationChanged(CombatStatsComponent stats) {
    for (Map.Entry<String, Integer> status : displayedStatusDurations.entrySet()) {
      StatusEffect effect = stats.getStatusEffect(status.getKey());
      if (effect == null || effect.getDuration() != status.getValue()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Rebuilds the status row from the player's active effects, one icon and turn count each.
   *
   * <p>Application/removal events mark the row dirty. Cached durations are checked separately
   * because ticking a live status does not always emit an event. Unchanged frames neither fetch a
   * new status snapshot nor rebuild actors. The whole row is hidden while no displayed debuff is
   * active.
   */
  private void updateStatusRow() {
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    if (statusIcons == null || stats == null) {
      return;
    }
    if (!statusRowDirty && !hasStatusDurationChanged(stats)) {
      return;
    }

    statusRowDirty = false;
    Map<String, Integer> durations = new HashMap<>();
    for (Map.Entry<String, Integer> status : stats.getStatusEffectDurations().entrySet()) {
      if (isKnownDebuff(status.getKey())) {
        durations.put(status.getKey(), status.getValue());
      }
    }
    if (durations.equals(displayedStatusDurations)) {
      return;
    }
    displayedStatusDurations = Map.copyOf(durations);

    boolean hasStatus = !durations.isEmpty();
    statusImage.setVisible(hasStatus);
    statusRow.setVisible(hasStatus);

    statusIcons.clear();
    if (!hasStatus) {
      return;
    }

    for (Map.Entry<String, Integer> status : durations.entrySet()) {
      Texture icon =
          ServiceLocator.getResourceService()
              .getAsset(IntentIcons.pathForStatus(status.getKey()), Texture.class);

      if (icon != null) {
        Label count = new Label(Integer.toString(status.getValue()), skin, STYLE_NAME_LARGE);
        count.setFontScale(FONT_SCALE);

        statusIcons.add(new Image(icon)).size(20f).padRight(2f);
        statusIcons.add(count).padRight(8f);
      }
    }
  }

  /**
   * Whether this status is one of the debuffs the row is meant to show.
   *
   * <p>The underlying map carries every status effect, including buffs from the player's own cards,
   * which would otherwise appear under a "Debuff" label.
   *
   * @param statusKey key the effect is stored under
   * @return true if the row should show it
   */
  private static boolean isKnownDebuff(String statusKey) {
    String effectName = statusKey.split(":")[0];

    return effectName.equals("SILENCE")
        || effectName.equals("DAMAGE_ON_CARD_PLAY")
        || effectName.equals("TAUNT");
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
    if (disposed) {
      return;
    }
    disposed = true;
    entity.getEvents().removeListener("statusEffectApplied", statusChangeListener);
    entity.getEvents().removeListener("statusEffectRemoved", statusChangeListener);
    super.dispose();
    table.remove();
  }
}

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
  private ProgressBar healthBar;
  private Label healthLabel;
  private Stack armourStack;
  private Label armourLabel;
  private Cell<Stack> armourCell;
  private static final String STYLE_NAME_WHITE = "white";
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
    entity.getEvents().addListener("updateArmour", this::updateArmourUI);
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

    float imageSideLength = 20f;

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
    Stack healthStack = new Stack();
    healthStack.add(healthBar);
    healthStack.add(healthLabel);

    // Add stacks and cell to table
    armourCell = table.add(armourStack).size(60f);
    updateArmourVisibility(armour);
    table.add(healthStack).width(150).height(30);

    table.pack();

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
    updatePosition();
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

  /** Updates the position of the player's stats, so they are displayed directly below the player */
  public void updatePosition() {
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();

    float playerX = position.x + scale.x / 2f;
    float playerY = position.y - 1.25f;

    Vector3 screenPosition = new Vector3(playerX, playerY, 0);

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
    healthBar.setRange(0, (float) maxHealth);
    healthBar.setValue((float) currentHealth);
    updateHealthBarColour();
  }

  /**
   * Updates the player's armour on the UI
   *
   * @param armour the player's armour
   */
  public void updateArmourUI(int armour) {
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
    if (disposed) {
      return;
    }
    disposed = true;
    entity.getEvents().removeListener("statusEffectApplied", statusChangeListener);
    entity.getEvents().removeListener("statusEffectRemoved", statusChangeListener);
    super.dispose();
    healthBar.remove();
    table.remove();
  }
}

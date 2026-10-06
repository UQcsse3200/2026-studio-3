package com.csse3200.game.components.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;

/**
 * Draws the enemy's telegraphed intent above the enemy, so the player can see what is coming before
 * choosing their cards.
 *
 * <p>Drawn in world space alongside the enemy sprite rather than as a screen-space HUD element, so
 * the icon follows the enemy it belongs to.
 */
public class EnemyIntentDisplay extends RenderComponent {
  private static final float ICON_SIZE = 0.5f;
  private static final float GAP_ABOVE_ENEMY = 0.2f;
  private static final float DURATION_TEXT_GAP = 0.05f;
  private static final float DURATION_TEXT_VERTICAL_OFFSET = 0.34f;
  private static final float FONT_SCALE = 0.025f;

  /**
   * Fraction of the entity's height that the visible sprite occupies. Enemy atlas frames are square
   * with transparent padding, so anchoring to the top of the entity box would float the icon well
   * above the art.
   */
  private static final float SPRITE_FILL = 0.9f;

  private EnemyIntent currentIntent;
  private BitmapFont durationFont;

  @Override
  public void create() {
    super.create();

    durationFont = new BitmapFont();
    durationFont.setUseIntegerPositions(false);
    durationFont.getData().setScale(FONT_SCALE);
    durationFont.setColor(Color.WHITE);

    entity.getEvents().addListener("intentChanged", this::onIntentChanged);
    entity.getEvents().addListener("enemyDefeated", this::onEnemyDefeated);
  }

  /**
   * Records the intent to draw.
   *
   * @param intent the enemy's newly rolled intent
   */
  private void onIntentChanged(EnemyIntent intent) {
    this.currentIntent = intent;
  }

  private void onEnemyDefeated() {
    this.currentIntent = null;
  }

  public EnemyIntent getCurrentIntent() {
    return currentIntent;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (currentIntent == null) {
      return;
    }

    ResourceService resourceService = ServiceLocator.getResourceService();
    if (resourceService == null) {
      return;
    }

    String iconPath = IntentIcons.pathFor(currentIntent.getType(), currentIntent.getEffectType());

    Texture icon = resourceService.getAsset(iconPath, Texture.class);
    if (icon == null) {
      return;
    }

    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();
    float iconX = position.x + (scale.x - ICON_SIZE) / 2f;
    float iconY = position.y + scale.y * SPRITE_FILL + GAP_ABOVE_ENEMY;

    batch.draw(icon, iconX, iconY, ICON_SIZE, ICON_SIZE);
    drawLabel(batch, iconX, iconY);
  }

  /**
   * Draws the number beside the intent icon: damage or armour for a hit, turns for an effect.
   *
   * @param batch sprite batch currently used by the render service
   * @param iconX horizontal position of the intent icon
   * @param iconY vertical position of the intent icon
   */
  private void drawLabel(SpriteBatch batch, float iconX, float iconY) {
    if (durationFont == null) {
      return;
    }

    String labelText = labelTextFor(currentIntent);
    if (labelText.isEmpty()) {
      return;
    }

    float textX = iconX + ICON_SIZE + DURATION_TEXT_GAP;
    float textY = iconY + DURATION_TEXT_VERTICAL_OFFSET;

    durationFont.draw(batch, labelText, textX, textY);
  }

  /**
   * Picks the number shown beside the icon.
   *
   * <p>Attacks and blocks show how much, so the player can decide whether to defend or push; every
   * other intent keeps showing how many turns its effect will last.
   *
   * @param intent intent being telegraphed
   * @return the number as text, or an empty string when there is nothing useful to show
   */
  static String labelTextFor(EnemyIntent intent) {
    if (intent == null) {
      return "";
    }

    return switch (intent.getType()) {
      case ATTACK, DEFEND -> intent.getValue() <= 0 ? "" : Integer.toString(intent.getValue());
      default -> durationTextFor(intent);
    };
  }

  /**
   * Converts an intent's finite duration into the text displayed beside its icon.
   *
   * <p>Zero and negative durations represent effects without a finite duration, so they produce no
   * text. This method does not depend on libGDX rendering state, allowing the display rule to be
   * tested without an OpenGL context.
   *
   * @param intent intent whose duration should be displayed
   * @return the positive duration as text, or an empty string when no duration should be displayed
   */
  static String durationTextFor(EnemyIntent intent) {
    if (intent == null || intent.getDuration() <= 0) {
      return "";
    }

    return Integer.toString(intent.getDuration());
  }

  @Override
  public void dispose() {
    super.dispose();

    if (durationFont != null) {
      durationFont.dispose();
      durationFont = null;
    }
  }
}

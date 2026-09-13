package com.csse3200.game.components.enemy;

import com.badlogic.gdx.graphics.Texture;
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

  private EnemyIntent currentIntent;

  @Override
  public void create() {
    super.create();
    entity.getEvents().addListener("intentChanged", this::onIntentChanged);
  }

  /**
   * Records the intent to draw.
   *
   * @param intent the enemy's newly rolled intent
   */
  private void onIntentChanged(EnemyIntent intent) {
    this.currentIntent = intent;
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

    Texture icon =
        resourceService.getAsset(IntentIcons.pathFor(currentIntent.getType()), Texture.class);
    if (icon == null) {
      return;
    }

    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();
    float x = position.x + (scale.x - ICON_SIZE) / 2f;
    float y = position.y + scale.y + GAP_ABOVE_ENEMY;

    batch.draw(icon, x, y, ICON_SIZE, ICON_SIZE);
  }
}

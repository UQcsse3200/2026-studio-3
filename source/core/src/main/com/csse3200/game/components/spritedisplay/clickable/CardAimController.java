package com.csse3200.game.components.spritedisplay.clickable;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import java.util.LinkedHashMap;
import java.util.Map;

/** Owns the visible aim preview for a battle and resolves its target on release. */
public final class CardAimController implements AimSession {
  private static final float SNAP_DISTANCE = 90f;
  private static final float TARGET_PADDING = 12f;

  private final Stage stage;
  private final Camera worldCamera;
  private final Map<String, Entity> targets;
  private final AimArrowActor arrow;
  private final Vector2 source = new Vector2();
  private boolean active;

  public CardAimController(Stage stage, Camera worldCamera, Map<String, Entity> targets) {
    this.stage = stage;
    this.worldCamera = worldCamera;
    this.targets = targets;
    arrow = new AimArrowActor();
    stage.addActor(arrow);
  }

  @Override
  public void begin(Vector2 cardPosition, Vector2 pointer) {
    source.set(cardPosition);
    active = true;
    update(pointer);
  }

  @Override
  public void update(Vector2 pointer) {
    if (!active) {
      return;
    }
    Map<String, Rectangle> bounds = currentBounds();
    String selected = EnemyTargetSelector.select(bounds, pointer, this::isAlive, SNAP_DISTANCE);
    Rectangle box = selected == null ? null : bounds.get(selected);
    Vector2 destination =
        box == null ? pointer : new Vector2(box.x + box.width / 2f, box.y + box.height / 2f);
    arrow.show(source, destination, box);
  }

  @Override
  public String release(Vector2 pointer) {
    if (!active) {
      return null;
    }
    Map<String, Rectangle> bounds = currentBounds();
    String selected = EnemyTargetSelector.select(bounds, pointer, this::isAlive, SNAP_DISTANCE);
    cancel();
    return selected;
  }

  @Override
  public void cancel() {
    active = false;
    arrow.setVisible(false);
  }

  public void dispose() {
    cancel();
    arrow.dispose();
  }

  private boolean isAlive(String id) {
    Entity target = targets.get(id);
    CombatStatsComponent stats =
        target == null ? null : target.getComponent(CombatStatsComponent.class);
    return stats != null && !stats.isDead();
  }

  private Map<String, Rectangle> currentBounds() {
    Map<String, Rectangle> bounds = new LinkedHashMap<>();
    targets.forEach((id, target) -> bounds.put(id, stageBounds(target)));
    return bounds;
  }

  private Rectangle stageBounds(Entity target) {
    Vector2 position = target.getPosition();
    Vector2 scale = target.getScale();
    Vector2 bottomLeft = project(position.x, position.y);
    Vector2 topRight = project(position.x + scale.x, position.y + scale.y);
    float left = Math.min(bottomLeft.x, topRight.x) - TARGET_PADDING;
    float bottom = Math.min(bottomLeft.y, topRight.y) - TARGET_PADDING;
    float width = Math.abs(topRight.x - bottomLeft.x) + TARGET_PADDING * 2f;
    float height = Math.abs(topRight.y - bottomLeft.y) + TARGET_PADDING * 2f;
    return new Rectangle(left, bottom, width, height);
  }

  private Vector2 project(float x, float y) {
    Vector3 screen = worldCamera.project(new Vector3(x, y, 0));
    return stage.screenToStageCoordinates(
        new Vector2(screen.x, Gdx.graphics.getHeight() - screen.y));
  }
}

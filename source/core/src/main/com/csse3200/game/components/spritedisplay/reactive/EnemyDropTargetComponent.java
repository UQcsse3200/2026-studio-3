package com.csse3200.game.components.spritedisplay.reactive;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.ServiceLocator;

public class EnemyDropTargetComponent extends Component {
  private static final boolean DEBUG_VISUAL = false;

  private final DragAndDrop dragAndDrop; // shared instance, passed in
  private final Camera worldCamera; // camera used to render the game world
  private Actor hitboxActor;
  private Image debugOverlay;
  private Texture debugTexture;
  private String id;

  // Tune these to roughly match the entity's on-screen sprite size.
  private static final float HITBOX_WIDTH_PX = 100f;
  private static final float HITBOX_HEIGHT_PX = 100f;

  public EnemyDropTargetComponent(DragAndDrop dragAndDrop, Camera worldCamera, String id) {
    this.dragAndDrop = dragAndDrop;
    this.worldCamera = worldCamera;
    this.id = id;
  }

  @Override
  public void create() {
    super.create();

    hitboxActor = new Actor();

    hitboxActor.setUserObject(id);

    if (DEBUG_VISUAL) {
      Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
      pixmap.setColor(1f, 0f, 0f, 0.35f);
      pixmap.fill();
      debugTexture = new Texture(pixmap);
      pixmap.dispose();

      debugOverlay = new Image(debugTexture);
      debugOverlay.setTouchable(Touchable.disabled); // must not intercept drag/drop itself
      ServiceLocator.getRenderService().getStage().addActor(debugOverlay);
    }

    syncBounds(); // set initial position/size from entity

    // Critical: DragAndDrop skips any target whose actor.getStage() == null.
    // An Actor with no parent is never "on" the stage, so it must be added here
    // even though it's invisible and only used for hit-testing.
    ServiceLocator.getRenderService().getStage().addActor(hitboxActor);

    dragAndDrop.addTarget(
        new DragAndDrop.Target(hitboxActor) {
          @Override
          public boolean drag(
              DragAndDrop.Source source,
              DragAndDrop.Payload payload,
              float x,
              float y,
              int pointer) {
            return true; // add validity checks against payload.getObject() if needed
          }

          @Override
          public void drop(
              DragAndDrop.Source source,
              DragAndDrop.Payload payload,
              float x,
              float y,
              int pointer) {
            // DragNDrop.dragStop dispatches the event on the source entity.
          }
        });
  }

  @Override
  public void update() {
    syncBounds(); // keep hitbox glued to entity's current position every frame
  }

  @Override
  public void dispose() {
    super.dispose();
    if (hitboxActor != null) {
      hitboxActor.remove();
    }
    if (debugOverlay != null) {
      debugOverlay.remove();
    }
    if (debugTexture != null) {
      debugTexture.dispose();
    }
  }

  /**
   * Projects the entity's world position into stage coordinates and sizes the hitbox actor
   * accordingly.
   *
   * <p>IMPORTANT: Camera.project(Vector3) does NOT flip Y to screen-down convention — it's a pure
   * NDC-to-viewport-pixel mapping, so its output is already Y-up, bottom-left origin. Since this
   * Stage uses a plain ScreenViewport spanning the whole window 1:1 (see Renderer), that output is
   * already directly usable as stage coordinates. Do NOT run it through screenToStageCoordinates()
   * — that applies an extra unwanted flip.
   */
  private void syncBounds() {
    Vector3 worldPos = new Vector3(entity.getCenterPosition().x, entity.getCenterPosition().y, 0);

    worldCamera.project(worldPos); // already Y-up, bottom-left origin pixels

    float stageX = worldPos.x - HITBOX_WIDTH_PX / 2f;
    float stageY = worldPos.y - HITBOX_HEIGHT_PX / 2f;

    hitboxActor.setBounds(stageX, stageY, HITBOX_WIDTH_PX, HITBOX_HEIGHT_PX);

    if (DEBUG_VISUAL && debugOverlay != null) {
      debugOverlay.setBounds(stageX, stageY, HITBOX_WIDTH_PX, HITBOX_HEIGHT_PX);
    }
  }
}

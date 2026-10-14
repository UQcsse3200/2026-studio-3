package com.csse3200.game.components.spritedisplay.reactive;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
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
  private DragAndDrop.Target target;
  private Image debugOverlay;
  private Texture debugTexture;
  private final String id;
  private final Vector3 worldPos = new Vector3(); // reused every frame by syncBounds
  private final Vector2 stagePos = new Vector2();

  // Tune these to roughly match the entity's on-screen sprite size, in stage units.
  private static final float HITBOX_WIDTH = 100f;
  private static final float HITBOX_HEIGHT = 100f;

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

    target =
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
        };
    dragAndDrop.addTarget(target);
  }

  @Override
  public void update() {
    syncBounds(); // keep hitbox glued to entity's current position every frame
  }

  @Override
  public void dispose() {
    super.dispose();
    // The shared DragAndDrop outlives this enemy; without this it keeps the target (and through it
    // this component and its entity) for the rest of the battle.
    if (target != null) {
      dragAndDrop.removeTarget(target);
    }
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
   * <p>Camera.project(Vector3) maps to window pixels with a bottom-left origin. The stage uses an
   * ExtendViewport (see Renderer), so window pixels only equal stage units at exactly 1280x800.
   * screenToStageCoordinates expects a top-left origin, hence the Y flip before converting.
   */
  private void syncBounds() {
    Vector2 centre = entity.getCenterPosition();
    worldCamera.project(worldPos.set(centre.x, centre.y, 0f));

    Stage stage = hitboxActor.getStage();
    if (stage == null) {
      stage = ServiceLocator.getRenderService().getStage();
    }
    stage.screenToStageCoordinates(stagePos.set(worldPos.x, Gdx.graphics.getHeight() - worldPos.y));

    float stageX = stagePos.x - HITBOX_WIDTH / 2f;
    float stageY = stagePos.y - HITBOX_HEIGHT / 2f;

    hitboxActor.setBounds(stageX, stageY, HITBOX_WIDTH, HITBOX_HEIGHT);

    if (DEBUG_VISUAL && debugOverlay != null) {
      debugOverlay.setBounds(stageX, stageY, HITBOX_WIDTH, HITBOX_HEIGHT);
    }
  }
}

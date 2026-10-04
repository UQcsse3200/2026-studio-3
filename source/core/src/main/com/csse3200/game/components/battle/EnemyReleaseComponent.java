package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

public class EnemyReleaseComponent extends Component {
  private static final float DURATION = 2;
  private float elapsed;
  private boolean started;
  private boolean poseRestored;
  // not sure about this colour yet.
  private static final Color RELEASE_COLOUR = new Color(1f, 0.72f, 0.32f, 1f);

  private AnimationRenderComponent animator;
  private final Color startingColour = new Color(Color.WHITE);
  private final Color currentColour = new Color();

  // this is for the rising effect
  private static final float RISE_START_TIME = 0.4f;
  private static final float RISE_DURATION = 1.0f;
  private static final float START_HEIGHT_RATIO = 0.3f;

  private float originalScaleX;
  private float originalScaleY;

  public boolean isFinished() {
    return started && elapsed >= DURATION;
  }

  public boolean isPlaying() {
    return started && !isFinished();
  }

  public float getElapsedTime() {
    return elapsed;
  }

  public float getProgress() {
    return Math.min(elapsed / DURATION, 1f);
  }

  @Override
  public void create() {
    super.create();
    animator = entity.getComponent(AnimationRenderComponent.class);
  }

  public void startRelease() {
    if (started) {
      return;
    }

    Vector2 originalScale = entity.getScale();
    originalScaleX = originalScale.x;
    originalScaleY = originalScale.y;

    startingColour.set(Color.WHITE);
    // Captures enemy tint when the 'death' effect starts.
    if (animator != null && animator.getActiveTint() != null) {
      startingColour.set(animator.getActiveTint());
    }

    elapsed = 0;
    started = true;
  }

  @Override
  public void update() {

    if (!started || isFinished()) {
      return;
    }

    elapsed = Math.min(elapsed + ServiceLocator.getTimeSource().getDeltaTime(), DURATION);

    if (animator != null && elapsed >= RISE_START_TIME) {
      if (!poseRestored && animator.hasAnimation("idle")) {
        animator.startAnimation("idle");
        entity.setScale(originalScaleX, originalScaleY * START_HEIGHT_RATIO);
        poseRestored = true;
      }

      if (poseRestored) {
        float riseProgress = Math.clamp((elapsed - RISE_START_TIME) / RISE_DURATION, 0f, 1f);
        float heightRatio = START_HEIGHT_RATIO + (1f - START_HEIGHT_RATIO) * riseProgress;

        entity.setScale(originalScaleX, originalScaleY * heightRatio);
      }
    }

    if (animator != null) {
      float progress = elapsed / DURATION;
      // using lerp to blend the colour over a duration instead of a cold switch
      currentColour.set(startingColour).lerp(RELEASE_COLOUR, progress);
      animator.setPersistentTint(currentColour);
    }
  }

  public float getOriginalScaleX() {
    return originalScaleX;
  }

  public float getOriginalScaleY() {
    return originalScaleY;
  }
}

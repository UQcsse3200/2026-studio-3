package com.csse3200.game.components.spritedisplay.clickable;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InOutOnTrigger extends Clickable {
  private Random random = new Random();
  private static final Logger logger = LoggerFactory.getLogger(InOutOnTrigger.class);

  // Store the target position (where the button should rest when visible)
  private float targetX;
  private float targetY;

  // Off-screen position (below the screen)
  private float offScreenY;

  // Animation duration in seconds
  private static final float ANIMATION_DURATION = 0.5f;

  // Track whether the button is currently animating
  private boolean isAnimating = false;

  public InOutOnTrigger(ClickableRecord record) {
    super(record);
    int screenHeight = Gdx.graphics.getHeight();
    this.targetX = record.x();
    this.targetY = screenHeight - record.y();
  }

  @Override
  public void create() {
    super.create();

    // Calculate off-screen position (just below the bottom of the screen)
    int screenHeight = Gdx.graphics.getHeight();
    offScreenY = -btn.getHeight() - 50; // 50px extra padding

    // Start the button off-screen
    btn.setPosition(targetX, offScreenY);

    logger.info("InOutOnTrigger created! Listening for 'up' and 'down'");

    // Listen for events that trigger the animation
    entity.getEvents().addListener("up", this::slideUp);
    entity.getEvents().addListener("down", this::slideDown);
  }

  private void slideUp() {
    if (isAnimating) return;

    if (Math.abs(btn.getY() - targetY) < 1f) {
      return;
    }

    float overshootAmount = 15f;
    float overshootY = targetY + overshootAmount;

    isAnimating = true;
    btn.setVisible(true);
    btn.setPosition(targetX, offScreenY); // Make sure it's visible
    btn.addAction(
        Actions.sequence(
            Actions.moveTo(targetX, offScreenY, 0f),
            Actions.moveTo(
                targetX,
                overshootY,
                ANIMATION_DURATION * 0.8f + random.nextFloat(),
                Interpolation.pow2Out),
            Actions.moveTo(
                targetX,
                targetY,
                ANIMATION_DURATION * 0.3f + random.nextFloat(),
                Interpolation.pow2Out),
            Actions.run(() -> isAnimating = false)));
  }

  /**
   * Slides the button out from its target position to off-screen. Triggered by the "down" event.
   */
  private void slideDown() {
    logger.info("down");
    btn.clearActions();
    isAnimating = false; // reset flag

    float startY = btn.getY();
    isAnimating = true;
    btn.addAction(
        Actions.sequence(
            // We don't need to snap to targetY; move from current position
            Actions.moveTo(targetX, offScreenY, ANIMATION_DURATION),
            Actions.run(
                () -> {
                  isAnimating = false;
                  btn.setVisible(false);
                })));
  }

  /** Optional: Override to change the animation speed. */
  public void setAnimationDuration(float seconds) {
    // Could add a setter if you want configurable speed
  }

  @Override
  public void draw() {
    if (this.getWidth() > 0 && this.getHeight() > 0) {
      btn.setSize(this.getWidth(), this.getHeight());
    }
  }

  @Override
  protected void onEnter() {
    // Cancel any ongoing animation (e.g., from a previous exit)
    btn.clearActions();

    // Move up by 10 pixels from current position
    // Using "moveBy" with a curved easing (slow in, fast out)
    btn.addAction(Actions.moveTo(targetX, targetY + 120, 0.3f, Interpolation.sineOut));
  }

  @Override
  protected void onExit() {
    // Cancel any ongoing animation
    btn.clearActions();

    // Move down by 10 pixels (back to original)
    // Use a slightly different curve for a nice feel
    btn.addAction(Actions.moveTo(targetX, targetY, 0.3f, Interpolation.sineIn));
  }
}

package com.csse3200.game.components.spritedisplay.clickable;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InOutOnTrigger extends Clickable {
  private Random random = new Random();
  private static final Logger logger = LoggerFactory.getLogger(InOutOnTrigger.class);

  // Store the target position (where the button should rest when visible)
  protected float targetX;
  protected float targetY;

  // Off-screen position (below the screen)
  private float offScreenY;

  // Animation duration in seconds
  private static final float ANIMATION_DURATION = 0.5f;

  // Track whether the button is currently animating
  protected boolean isAnimating = false;

  // Fired on the shared entity whenever one of these widgets is hovered/unhovered, so overlapping
  // siblings (e.g. the rest of a fanned hand) can slide out of the way to reveal it. Carries the
  // hovered widget itself and whether it's now hovered (true) or just stopped being (false).
  private static final String HOVER_EVENT = "handCardHover";

  // How far a sibling slides away from a hovered card, so its overlapped edge becomes visible.
  private static final float SIBLING_SPREAD_PX = 70f;

  // Duration of the sibling-spread slide, snappier than the full up/down slide.
  private static final float SPREAD_DURATION = 0.2f;

  public InOutOnTrigger(ClickableRecord rec) {
    super(rec);
    this.targetX = rec.x();
    this.targetY = rec.y(); // temporarily store the y JSON value
  }

  @Override
  public void create() {
    super.create();

    // Calculate off-screen position (just below the bottom of the screen)
    offScreenY = -btn.getHeight() - 50; // 50px extra padding

    // Listen for events that trigger the animation
    entity.getEvents().addListener("up", this::slideUp);
    entity.getEvents().addListener("down", this::slideDown);

    // Every widget of this type listens for every other one's hover state, so the group can
    // spread apart around whichever one is currently hovered.
    entity.getEvents().addListener(HOVER_EVENT, this::onSiblingHoverChanged);
  }

  /** Snap straight to the visible resting position, e.g. for a card drawn mid-turn. */
  @Override
  public void showNow() {
    btn.clearActions();
    isAnimating = false;
    btn.setVisible(true);
    btn.setPosition(targetX, targetY);
    setInteractable(true);
  }

  /**
   * Snap straight to the hidden off-screen position — the rebuild-during-the-enemy's-turn
   * counterpart to {@link #showNow()}, used so a widget built while the hand should still be down
   * (e.g. a cooldown retrieval mid "enemy thinking" pause) doesn't pop into view ahead of the
   * delayed "up" animation.
   */
  @Override
  public void hideNow() {
    btn.clearActions();
    isAnimating = false;
    btn.setVisible(false);
    btn.setPosition(targetX, offScreenY);
    setInteractable(false);
  }

  private void slideUp() {
    if (btn.getStage() == null) return;

    setInteractable(true);
    if (!isAnimating && Math.abs(btn.getY() - targetY) < 1f) {
      return;
    }

    float overshootAmount = 15f;
    float overshootY = targetY + overshootAmount;

    btn.clearActions();
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
   * Interactivity is blocked immediately, not just once the slide finishes, so a click
   * mid-animation (or during the enemy's whole turn) can't sneak a card play through.
   */
  protected void slideDown() {
    setInteractable(false);
    if (btn.getStage() == null) return;
    logger.info("down");
    btn.clearActions();
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
  public void onAddedToStage(Stage stage) {
    float stageHeight = btn.getStage().getViewport().getWorldHeight();
    this.targetY = stageHeight - targetY;
  }

  @Override
  protected void onEnter() {
    // Cancel any ongoing animation (e.g., from a previous exit)
    btn.clearActions();

    // Move up by 10 pixels from current position
    // Using "moveBy" with a curved easing (slow in, fast out)
    btn.addAction(Actions.moveTo(targetX, targetY + 120, 0.3f, Interpolation.sineOut));

    entity.getEvents().trigger(HOVER_EVENT, this, true);
  }

  @Override
  public void draw() {
    applySizeAndRotation();
  }

  @Override
  protected void onExit() {
    // Cancel any ongoing animation
    btn.clearActions();

    // Move down by 10 pixels (back to original)
    // Use a slightly different curve for a nice feel
    btn.addAction(Actions.moveTo(targetX, targetY, 0.3f, Interpolation.sineIn));

    entity.getEvents().trigger(HOVER_EVENT, this, false);
  }

  /**
   * Reacts to a sibling widget (sharing this one's trigger, e.g. every "playCard" hand slot)
   * becoming hovered or unhovered. Slides this widget sideways, away from the hovered one, so its
   * overlapped edge is no longer hidden underneath it; moves back to {@link #targetX} once nothing
   * is hovered. Does nothing for a widget reacting to its own hover (that's handled by {@link
   * #onEnter()}/{@link #onExit()} already) or to a widget with a different trigger (e.g. the hand
   * shouldn't react to the End Turn button).
   */
  private void onSiblingHoverChanged(InOutOnTrigger sibling, boolean hovering) {
    if (sibling == this || !trigger.equals(sibling.trigger)) {
      return;
    }

    float offset = 0f;
    if (hovering) {
      float direction = Math.signum(targetX - sibling.targetX);
      offset = (direction == 0f ? 1f : direction) * SIBLING_SPREAD_PX;
    }
    // Without clearing first, a quick exit-then-enter while the mouse crosses overlapping cards
    // (moving off one card straight onto the next) stacks a "snap back to center" moveTo and a
    // "spread to new offset" moveTo on top of each other — libGDX runs both in parallel rather
    // than queuing them, so they fight over the actor's position every frame and jitter.
    btn.clearActions();
    btn.addAction(
        Actions.moveTo(targetX + offset, targetY, SPREAD_DURATION, Interpolation.sineOut));
  }
}

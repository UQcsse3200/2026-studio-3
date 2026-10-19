package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import java.util.List;
import java.util.Objects;

/** Controls presentation-only animation for the post-battle victory reward screen. */
final class VictoryAnimationController {
  enum State {
    INITIAL,
    ENTERING,
    READY,
    DISPOSED
  }

  private static final float PANEL_DELAY = 0.12f;
  private static final float TITLE_DELAY = 0.22f;
  private static final float LETTER_STAGGER = 0.07f;
  private static final float LETTER_DURATION = 0.22f;
  private static final float WING_DELAY = 0.24f;
  private static final float WING_OPEN_DURATION = 0.44f;
  private static final float SUBTITLE_DELAY = 0.76f;
  private static final float CARD_DELAY = 0.91f;
  private static final float CARD_STAGGER = 0.1f;
  private static final float CARD_DURATION = 0.28f;
  private static final float CARD_RISE_DISTANCE = 18f;
  private static final float HOVER_DURATION = 0.12f;
  private static final float HOVER_SCALE = 1.045f;
  private static final float MUTED_ALPHA = 0.68f;

  private final Actor background;
  private final Actor scrim;
  private final Actor panel;
  private final Actor title;
  private final List<Actor> titleLetters;
  private final Actor subtitle;
  private final Actor sunPulse;
  private final List<Actor> wings;
  private final List<Actor> particles;
  private final List<Actor> rewardCards;
  private State state = State.INITIAL;
  private Actor hoveredCard;

  VictoryAnimationController(
      Actor background, Actor scrim, Actor panel, Actor title, List<? extends Actor> rewardCards) {
    this(
        background,
        scrim,
        panel,
        title,
        List.of(title),
        new Actor(),
        new Actor(),
        List.of(),
        List.of(),
        rewardCards);
  }

  VictoryAnimationController(
      Actor background,
      Actor scrim,
      Actor panel,
      Actor title,
      List<? extends Actor> titleLetters,
      Actor subtitle,
      Actor sunPulse,
      List<? extends Actor> particles,
      List<? extends Actor> rewardCards) {
    this(
        background,
        scrim,
        panel,
        title,
        titleLetters,
        subtitle,
        sunPulse,
        List.of(),
        particles,
        rewardCards);
  }

  VictoryAnimationController(
      Actor background,
      Actor scrim,
      Actor panel,
      Actor title,
      List<? extends Actor> titleLetters,
      Actor subtitle,
      Actor sunPulse,
      List<? extends Actor> wings,
      List<? extends Actor> particles,
      List<? extends Actor> rewardCards) {
    this.background = Objects.requireNonNull(background, "background cannot be null");
    this.scrim = Objects.requireNonNull(scrim, "scrim cannot be null");
    this.panel = Objects.requireNonNull(panel, "panel cannot be null");
    this.title = Objects.requireNonNull(title, "title cannot be null");
    this.titleLetters = List.copyOf(titleLetters);
    this.subtitle = Objects.requireNonNull(subtitle, "subtitle cannot be null");
    this.sunPulse = Objects.requireNonNull(sunPulse, "sunPulse cannot be null");
    this.wings = List.copyOf(wings);
    this.particles = List.copyOf(particles);
    this.rewardCards = List.copyOf(rewardCards);
  }

  /** Starts the entrance sequence once. Repeated calls are ignored. */
  void playEntrance() {
    if (state != State.INITIAL) {
      return;
    }
    state = State.ENTERING;

    prepare(background, 1f);
    prepare(scrim, 1f);
    prepare(panel, 1f);
    title.clearActions();
    title.getColor().a = 1f;
    title.setScale(1f);
    for (Actor letter : titleLetters) {
      prepare(letter, 0.86f);
    }
    prepare(subtitle, 0.97f);
    prepare(sunPulse, 0.9f);
    for (int index = 0; index < wings.size(); index++) {
      Actor wing = wings.get(index);
      wing.clearActions();
      wing.getColor().a = 1f;
      wing.setScale(0.96f);
      boolean left = index == 0;
      wing.setRotation(left ? -42f : 42f);
      wing.addAction(
          Actions.sequence(
              Actions.delay(WING_DELAY),
              Actions.parallel(
                  Actions.rotateTo(left ? 5f : -5f, WING_OPEN_DURATION, Interpolation.sineOut),
                  Actions.scaleTo(1f, 1f, WING_OPEN_DURATION, Interpolation.sineOut)),
              Actions.rotateTo(0f, 0.12f, Interpolation.sineOut)));
    }
    for (Actor particle : particles) {
      prepare(particle, 0.7f);
    }

    background.addAction(Actions.fadeIn(0.25f, Interpolation.fade));
    scrim.addAction(
        Actions.sequence(Actions.delay(0.08f), Actions.fadeIn(0.3f, Interpolation.fade)));
    panel.addAction(
        Actions.sequence(Actions.delay(PANEL_DELAY), Actions.fadeIn(0.35f, Interpolation.pow2Out)));
    for (int index = 0; index < titleLetters.size(); index++) {
      Actor letter = titleLetters.get(index);
      letter.addAction(
          Actions.sequence(
              Actions.delay(TITLE_DELAY + index * LETTER_STAGGER),
              Actions.moveBy(0f, -7f),
              Actions.parallel(
                  Actions.fadeIn(LETTER_DURATION, Interpolation.fade),
                  Actions.scaleTo(1f, 1f, LETTER_DURATION, Interpolation.pow2Out),
                  Actions.moveBy(0f, 7f, LETTER_DURATION, Interpolation.sineOut))));
    }

    subtitle.addAction(
        Actions.sequence(
            Actions.delay(SUBTITLE_DELAY),
            Actions.parallel(
                Actions.fadeIn(0.22f, Interpolation.fade),
                Actions.scaleTo(1f, 1f, 0.22f, Interpolation.sineOut))));

    sunPulse.addAction(
        Actions.sequence(
            Actions.delay(0.43f),
            Actions.parallel(
                Actions.alpha(0.34f, 0.13f, Interpolation.fade),
                Actions.scaleTo(1.055f, 1.055f, 0.13f, Interpolation.pow2Out)),
            Actions.parallel(
                Actions.alpha(0.22f, 0.11f, Interpolation.fade),
                Actions.scaleTo(1f, 1f, 0.11f, Interpolation.sineOut)),
            Actions.fadeOut(0.27f, Interpolation.fade)));

    for (int index = 0; index < particles.size(); index++) {
      Actor particle = particles.get(index);
      particle.addAction(
          Actions.sequence(
              Actions.delay(0.58f + index * 0.025f),
              Actions.parallel(
                  Actions.alpha(0.62f, 0.16f, Interpolation.fade),
                  Actions.scaleTo(1f, 1f, 0.16f, Interpolation.sineOut),
                  Actions.moveBy(0f, 6f, 0.16f, Interpolation.sineOut)),
              Actions.parallel(
                  Actions.fadeOut(0.3f, Interpolation.fade),
                  Actions.moveBy(0f, 8f, 0.3f, Interpolation.sineOut))));
    }

    for (int index = 0; index < rewardCards.size(); index++) {
      Actor card = rewardCards.get(index);
      prepare(card, 0.9f);
      card.setTouchable(Touchable.disabled);
      card.addAction(
          Actions.sequence(
              Actions.delay(CARD_DELAY + index * CARD_STAGGER),
              Actions.moveBy(0f, -CARD_RISE_DISTANCE),
              Actions.parallel(
                  Actions.fadeIn(CARD_DURATION, Interpolation.fade),
                  Actions.scaleTo(1f, 1f, CARD_DURATION, Interpolation.pow2Out),
                  Actions.moveBy(0f, CARD_RISE_DISTANCE, CARD_DURATION, Interpolation.sineOut))));
    }

    float readyDelay =
        rewardCards.isEmpty()
            ? SUBTITLE_DELAY + 0.22f
            : CARD_DELAY + (rewardCards.size() - 1) * CARD_STAGGER + CARD_DURATION;
    panel.addAction(Actions.sequence(Actions.delay(readyDelay), Actions.run(this::markReady)));
  }

  /** Highlights one reward choice without changing its reward data or click behaviour. */
  void showHover(Actor card) {
    if (state != State.READY
        || card == null
        || !rewardCards.contains(card)
        || card == hoveredCard) {
      return;
    }
    hoveredCard = card;
    for (Actor rewardCard : rewardCards) {
      rewardCard.clearActions();
      boolean selected = rewardCard == card;
      rewardCard.addAction(
          Actions.parallel(
              Actions.alpha(selected ? 1f : MUTED_ALPHA, HOVER_DURATION, Interpolation.fade),
              Actions.scaleTo(
                  selected ? HOVER_SCALE : 1f,
                  selected ? HOVER_SCALE : 1f,
                  HOVER_DURATION,
                  Interpolation.sineOut)));
    }
  }

  /** Restores the neutral reward-choice presentation. */
  void clearHover(Actor card) {
    if (state != State.READY || card == null || card != hoveredCard) {
      return;
    }
    hoveredCard = null;
    for (Actor rewardCard : rewardCards) {
      rewardCard.clearActions();
      rewardCard.addAction(
          Actions.parallel(
              Actions.alpha(1f, HOVER_DURATION, Interpolation.fade),
              Actions.scaleTo(1f, 1f, HOVER_DURATION, Interpolation.sineOut)));
    }
  }

  /** Stops owned animation actions and prevents later state changes. */
  void dispose() {
    if (state == State.DISPOSED) {
      return;
    }
    state = State.DISPOSED;
    hoveredCard = null;
    background.clearActions();
    scrim.clearActions();
    panel.clearActions();
    title.clearActions();
    subtitle.clearActions();
    sunPulse.clearActions();
    for (Actor wing : wings) {
      wing.clearActions();
    }
    for (Actor letter : titleLetters) {
      letter.clearActions();
    }
    for (Actor particle : particles) {
      particle.clearActions();
    }
    for (Actor card : rewardCards) {
      card.clearActions();
    }
  }

  State getState() {
    return state;
  }

  private void prepare(Actor actor, float initialScale) {
    actor.clearActions();
    actor.getColor().a = 0f;
    actor.setOrigin(actor.getWidth() / 2f, actor.getHeight() / 2f);
    actor.setScale(initialScale);
  }

  private void markReady() {
    if (state != State.ENTERING) {
      return;
    }
    state = State.READY;
    for (Actor card : rewardCards) {
      card.setTouchable(Touchable.enabled);
    }
  }
}

package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.rewards.RewardType;
import java.util.Objects;

/** Presentation-only destinations and brief claim feedback for the three battle reward types. */
final class RewardClaimFeedback {
  private record TrailDotSpec(
      String name,
      Color colour,
      Vector2 center,
      Vector2 size,
      float opacity,
      Vector2 drift,
      float duration) {}

  private static final float ICON_X = 24f;
  private static final float ICON_SIZE = 56f;
  private static final float[] BATTLE_BUTTON_Y_FROM_TOP = {180f, 239f, 299f};
  private static final float FLIGHT_DURATION = 0.85f;
  private static final float FEEDBACK_DURATION = 0.36f;
  private static final float TRAIL_INTERVAL = 0.035f;
  private static final int TRAIL_SEGMENTS = 18;
  private static final Color GOLD_TRAIL = Color.valueOf("FFE7A1");
  private static final Color ITEM_TRAIL = Color.valueOf("FFC27A");
  private static final Color CARD_TRAIL = Color.valueOf("C6E9FF");

  private final Stage stage;
  private final Group overlay;
  private final Image cardDestination;
  private final Image itemDestination;
  private final Image goldDestination;
  private final TextureRegion goldRegion;
  private final Skin skin;
  private boolean playing;

  RewardClaimFeedback(Stage stage, Skin skin, Texture battleIcons, Texture goldIcon) {
    this.stage = Objects.requireNonNull(stage);
    this.skin = Objects.requireNonNull(skin);
    Objects.requireNonNull(battleIcons);
    Objects.requireNonNull(goldIcon);

    overlay =
        new Group() {
          @Override
          public void act(float delta) {
            setSize(stage.getWidth(), stage.getHeight());
            layoutDestinations();
            super.act(delta);
          }
        };
    overlay.setTouchable(Touchable.disabled);
    overlay.setSize(stage.getWidth(), stage.getHeight());

    int cellWidth = battleIcons.getWidth() / 3;
    int horizontalPadding = Math.round(cellWidth * 0.125f);
    int verticalPadding = Math.round(battleIcons.getHeight() * 0.125f);
    cardDestination =
        icon(
            new TextureRegion(
                battleIcons,
                horizontalPadding,
                verticalPadding,
                cellWidth - 2 * horizontalPadding,
                battleIcons.getHeight() - 2 * verticalPadding));
    itemDestination =
        icon(
            new TextureRegion(
                battleIcons,
                cellWidth + horizontalPadding,
                verticalPadding,
                cellWidth - 2 * horizontalPadding,
                battleIcons.getHeight() - 2 * verticalPadding));
    int goldCrop = (int) (Math.min(goldIcon.getWidth(), goldIcon.getHeight()) * 0.87f);
    goldRegion =
        new TextureRegion(
            goldIcon,
            (goldIcon.getWidth() - goldCrop) / 2,
            (goldIcon.getHeight() - goldCrop) / 2,
            goldCrop,
            goldCrop);
    goldDestination = icon(goldRegion);
    overlay.addActor(cardDestination);
    overlay.addActor(itemDestination);
    overlay.addActor(goldDestination);
    layoutDestinations();
    stage.addActor(overlay);
  }

  /** Runs after reward mutation and the autosave callback; only navigation waits for this. */
  void play(
      RewardType type, TextureRegion rewardArt, Actor source, int amount, Runnable onFinished) {
    if (playing) {
      return;
    }
    playing = true;
    // Card selection is added later than the reward panel; keep the flying copy above it.
    overlay.toFront();
    Image destination = destinationFor(type);
    Vector2 start = stageCenter(source);
    Vector2 end = stageCenter(destination);

    Image flying = icon(type == RewardType.GOLD ? goldRegion : rewardArt);
    float width = type == RewardType.CARD ? 46f : 54f;
    float height = type == RewardType.CARD ? 62f : 54f;
    flying.setSize(width, height);
    flying.setPosition(start.x - width / 2f, start.y - height / 2f);
    flying.setOrigin(width / 2f, height / 2f);
    overlay.addActor(flying);
    flying.addAction(trailEmitter(type, start, end));

    flying.addAction(
        Actions.sequence(
            Actions.parallel(
                Actions.moveTo(
                    end.x - width / 2f, end.y - height / 2f, FLIGHT_DURATION, Interpolation.sine),
                Actions.scaleTo(0.58f, 0.58f, FLIGHT_DURATION, Interpolation.sine)),
            Actions.run(
                () -> {
                  flying.remove();
                  showImpact(destination, amount);
                })));

    overlay.addAction(
        Actions.sequence(
            Actions.delay(FLIGHT_DURATION + FEEDBACK_DURATION), Actions.run(onFinished)));
  }

  void dispose() {
    overlay.clearActions();
    overlay.remove();
  }

  Group getOverlay() {
    return overlay;
  }

  Image getDestination(RewardType type) {
    return destinationFor(type);
  }

  private Image destinationFor(RewardType type) {
    return switch (type) {
      case CARD -> cardDestination;
      case ITEM -> itemDestination;
      case GOLD -> goldDestination;
    };
  }

  private void layoutDestinations() {
    Image[] icons = {cardDestination, itemDestination, goldDestination};
    for (int i = 0; i < icons.length; i++) {
      icons[i].setPosition(ICON_X, stage.getHeight() - BATTLE_BUTTON_Y_FROM_TOP[i]);
    }
  }

  private Image icon(TextureRegion region) {
    Image image = new Image(region);
    image.setScaling(Scaling.fit);
    image.setSize(ICON_SIZE, ICON_SIZE);
    image.setOrigin(ICON_SIZE / 2f, ICON_SIZE / 2f);
    image.setTouchable(Touchable.disabled);
    return image;
  }

  /** Dense dots near the flying reward spread and thin out as they fall behind it. */
  private Action trailEmitter(RewardType type, Vector2 start, Vector2 end) {
    Vector2 direction = new Vector2(end).sub(start).nor();
    return new Action() {
      private float elapsed;
      private int emitted;

      @Override
      public boolean act(float delta) {
        elapsed += delta;
        while (emitted < TRAIL_SEGMENTS && elapsed >= (emitted + 1) * TRAIL_INTERVAL) {
          Vector2 current = flightPoint(start, end, (emitted + 1) * TRAIL_INTERVAL);
          emitTrail(type, current, direction, emitted);
          emitted++;
        }
        return emitted == TRAIL_SEGMENTS;
      }
    };
  }

  private static final String REWARD_STAR_PREFIX = "rewardStar";

  private Vector2 flightPoint(Vector2 start, Vector2 end, float elapsed) {
    return new Vector2(start).lerp(end, Interpolation.sine.apply(elapsed / FLIGHT_DURATION));
  }

  private void emitTrail(RewardType type, Vector2 current, Vector2 direction, int index) {
    Color colour = trailColour(type);
    emitTrailSegments(type, colour, current, direction, index);
    emitRewardStar(type, current, direction, index);
  }

  private Color trailColour(RewardType type) {
    return switch (type) {
      case GOLD -> GOLD_TRAIL;
      case ITEM -> ITEM_TRAIL;
      case CARD -> CARD_TRAIL;
    };
  }

  private void emitTrailSegments(
      RewardType type, Color colour, Vector2 current, Vector2 direction, int index) {
    for (int i = 0; i < 6; i++) {
      // Three bright points hug the reward; distant ones disperse and appear less often.
      if (!shouldSkipTrailSegment(i, index)) {
        emitTrailSegment(type, colour, current, direction, i);
      }
    }
  }

  private void emitRewardStar(RewardType type, Vector2 current, Vector2 direction, int index) {
    if (index % 3 != 0) {
      return;
    }

    float x = current.x - direction.x * 15f;
    float y = current.y - direction.y * 15f;
    addTrailDot(
        new TrailDotSpec(
            REWARD_STAR_PREFIX + type,
            Color.WHITE,
            new Vector2(x, y),
            new Vector2(12f, 2f),
            0.85f,
            new Vector2(),
            0.17f));
    addTrailDot(
        new TrailDotSpec(
            REWARD_STAR_PREFIX + type,
            Color.WHITE,
            new Vector2(x, y),
            new Vector2(2f, 12f),
            0.85f,
            new Vector2(),
            0.17f));
    addTrailDot(
        new TrailDotSpec(
            REWARD_STAR_PREFIX + type,
            Color.WHITE,
            new Vector2(x, y),
            new Vector2(3f, 3f),
            1f,
            new Vector2(),
            0.17f));
  }

  private boolean shouldSkipTrailSegment(int step, int index) {
    return (step == 3 && index % 2 != 0)
        || (step == 4 && index % 3 != 0)
        || (step == 5 && index % 4 != 0);
  }

  private void emitTrailSegment(
      RewardType type, Color colour, Vector2 current, Vector2 direction, int i) {
    float distance = 5f + i * 8f;
    float sideways = (i % 2 == 0 ? 1f : -1f) * (2f + i * 4f);
    float size = 4f;
    if (i < 3) {
      size = type == RewardType.GOLD ? 7f : 6f;
    }
    float width = type == RewardType.CARD ? size * 0.75f : size;
    float height = type == RewardType.CARD ? size * 1.4f : size;
    float x = current.x - direction.x * distance - direction.y * sideways;
    float y = current.y - direction.y * distance + direction.x * sideways;
    float duration = 0.16f + i * 0.014f;
    float driftX = -direction.x * (7f + i * 3f) - direction.y * sideways;
    float driftY = -direction.y * (7f + i * 3f) + direction.x * sideways;
    if (i < 3) {
      addTrailDot(
          new TrailDotSpec(
              "rewardGlow" + type,
              colour,
              new Vector2(x, y),
              new Vector2(width + 7f, height + 7f),
              0.2f,
              new Vector2(driftX, driftY),
              duration));
    }
    addTrailDot(
        new TrailDotSpec(
            "rewardTrail" + type,
            colour,
            new Vector2(x, y),
            new Vector2(width, height),
            Math.max(0.48f, 1f - i * 0.1f),
            new Vector2(driftX, driftY),
            duration));
  }

  private void addTrailDot(TrailDotSpec spec) {
    Image dot = new Image(skin.getDrawable("white"));
    dot.setName(spec.name());
    dot.setTouchable(Touchable.disabled);
    dot.setColor(spec.colour());
    dot.getColor().a = spec.opacity();
    dot.setSize(spec.size().x, spec.size().y);
    dot.setPosition(spec.center().x - spec.size().x / 2f, spec.center().y - spec.size().y / 2f);
    overlay.addActor(dot);
    dot.toBack();
    dot.addAction(
        Actions.sequence(
            Actions.parallel(
                Actions.moveBy(spec.drift().x, spec.drift().y, spec.duration()),
                Actions.fadeOut(spec.duration())),
            Actions.removeActor()));
  }

  private Vector2 stageCenter(Actor actor) {
    if (actor == null || actor.getStage() == null) {
      return new Vector2(stage.getWidth() / 2f, stage.getHeight() / 2f);
    }
    return actor.localToStageCoordinates(
        new Vector2(actor.getWidth() / 2f, actor.getHeight() / 2f));
  }

  private void showImpact(Image destination, int amount) {
    destination.clearActions();
    destination.setScale(1f);
    destination.addAction(
        Actions.sequence(
            Actions.scaleTo(1.16f, 1.16f, 0.1f, Interpolation.sineOut),
            Actions.scaleTo(1f, 1f, 0.16f, Interpolation.sineOut)));

    Label.LabelStyle style = new Label.LabelStyle(skin.get("small", Label.LabelStyle.class));
    style.fontColor = Color.valueOf("F3D292");
    Label count = new Label("+" + amount, style);
    count.pack();
    count.setTouchable(Touchable.disabled);
    count.setPosition(ICON_X + ICON_SIZE + 8f, destination.getY() + 12f);
    overlay.addActor(count);
    count.addAction(
        Actions.sequence(
            Actions.parallel(
                Actions.moveBy(0f, 9f, FEEDBACK_DURATION, Interpolation.sineOut),
                Actions.fadeOut(FEEDBACK_DURATION, Interpolation.fade)),
            Actions.removeActor()));
  }
}

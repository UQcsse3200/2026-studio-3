package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
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
  private static final float ICON_X = 24f;
  private static final float ICON_SIZE = 56f;
  private static final float[] BATTLE_BUTTON_Y_FROM_TOP = {180f, 239f, 299f};
  private static final float FLIGHT_DURATION = 0.85f;
  private static final float FEEDBACK_DURATION = 0.36f;

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

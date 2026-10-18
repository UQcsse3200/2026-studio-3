package com.csse3200.game.components.status;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import java.util.function.Supplier;

/**
 * A hover panel explaining one status effect: its name, what it does and how long it lasts.
 *
 * <p>Styled to match the card keyword tooltip, so both read as one system. While shown, the panel
 * asks its content supplier for fresh text every frame, so the remaining turns stay current. When
 * the supplier returns null, meaning the status has ended, the panel hides itself.
 *
 * <p>The panel never receives input, so it cannot block clicks on the actors underneath it.
 */
public final class StatusTooltip extends Table {
  private static final float TEXT_WIDTH = 240f;
  private static final float PADDING = 12f;
  private static final float GAP = 8f;
  private static final String LABEL_STYLE = "small";
  private static final Color PANEL_COLOUR = new Color(0.04f, 0.06f, 0.09f, 0.98f);
  private static final Color DURATION_COLOUR = new Color(0.72f, 0.76f, 0.82f, 1f);

  /**
   * What the panel shows for one status.
   *
   * @param title status name, shown as the heading
   * @param description what the status does
   * @param duration how long the status lasts, such as "2 turns remaining"
   */
  public record Content(String title, String description, String duration) {}

  private final Label titleLabel;
  private final Label descriptionLabel;
  private final Label durationLabel;
  private final Vector2 corner = new Vector2();
  private Actor anchor;
  private Supplier<Content> source;
  private Content shown;

  /**
   * Creates a hidden panel that borrows its fonts and drawables from the given skin.
   *
   * @param skin the shared UI skin; it must provide a "small" label style
   */
  public StatusTooltip(Skin skin) {
    Label.LabelStyle baseStyle = skin.get(LABEL_STYLE, Label.LabelStyle.class);

    Label.LabelStyle titleStyle = new Label.LabelStyle(baseStyle);
    titleStyle.fontColor = new Color(Color.GOLD);
    Label.LabelStyle descriptionStyle = new Label.LabelStyle(baseStyle);
    descriptionStyle.fontColor = new Color(Color.WHITE);
    Label.LabelStyle durationStyle = new Label.LabelStyle(baseStyle);
    durationStyle.fontColor = new Color(DURATION_COLOUR);

    titleLabel = new Label("", titleStyle);
    titleLabel.setWrap(true);
    descriptionLabel = new Label("", descriptionStyle);
    descriptionLabel.setWrap(true);
    durationLabel = new Label("", durationStyle);
    durationLabel.setWrap(true);

    setBackground(skin.newDrawable("white", PANEL_COLOUR));
    pad(PADDING);
    defaults().width(TEXT_WIDTH).left();
    add(titleLabel);
    row();
    add(descriptionLabel).padTop(4f);
    row();
    add(durationLabel).padTop(6f);

    setTouchable(Touchable.disabled);
    setVisible(false);
  }

  /**
   * Shows this panel while the pointer is over the given actor.
   *
   * @param target the actor to explain, normally a status icon
   * @param content supplies the text to show; returning null means there is nothing to explain
   */
  public void attachTo(Actor target, Supplier<Content> content) {
    target.addListener(
        new InputListener() {
          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            // pointer is -1 for mouse movement; ignore touches and moves within the target.
            if (pointer == -1 && (fromActor == null || !fromActor.isDescendantOf(target))) {
              showFor(target, content);
            }
          }

          @Override
          public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
            if (pointer == -1 && (toActor == null || !toActor.isDescendantOf(target))) {
              hideFor(target);
            }
          }
        });
  }

  /** Hides the panel and removes it from its stage. */
  public void hide() {
    anchor = null;
    source = null;
    shown = null;
    setVisible(false);
    remove();
  }

  @Override
  public void act(float delta) {
    super.act(delta);
    if (anchor == null) {
      return;
    }
    if (anchor.getStage() != getStage() || !anchor.isVisible()) {
      hide();
      return;
    }
    Content content = source.get();
    if (content == null) {
      hide();
      return;
    }
    if (!content.equals(shown)) {
      setContent(content);
    }
    updatePosition();
  }

  private void showFor(Actor target, Supplier<Content> content) {
    Stage stage = target.getStage();
    Content current = content.get();
    if (stage == null || current == null) {
      return;
    }
    anchor = target;
    source = content;
    setContent(current);
    stage.addActor(this);
    setVisible(true);
    updatePosition();
  }

  private void hideFor(Actor target) {
    if (anchor == target) {
      hide();
    }
  }

  private void setContent(Content content) {
    shown = content;
    titleLabel.setText(content.title());
    descriptionLabel.setText(content.description());
    durationLabel.setText(content.duration());
    pack();
  }

  /**
   * Centres the panel above the anchor, or below it when there is no room above, and keeps it
   * inside the stage.
   */
  private void updatePosition() {
    Stage stage = getStage();
    if (stage == null) {
      return;
    }

    corner.set(0f, 0f);
    anchor.localToStageCoordinates(corner);
    float left = corner.x;
    float bottom = corner.y;
    corner.set(anchor.getWidth(), anchor.getHeight());
    anchor.localToStageCoordinates(corner);
    float right = corner.x;
    float top = corner.y;

    float panelX = (left + right) / 2f - getWidth() / 2f;
    float panelY = top + GAP;
    if (panelY + getHeight() > stage.getHeight()) {
      panelY = bottom - GAP - getHeight();
    }

    setPosition(
        MathUtils.clamp(panelX, 0f, Math.max(0f, stage.getWidth() - getWidth())),
        MathUtils.clamp(panelY, 0f, Math.max(0f, stage.getHeight() - getHeight())));
  }
}

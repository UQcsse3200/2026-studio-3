package com.csse3200.game.components.cards;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import com.csse3200.game.cards.CardKeyword;
import java.util.List;

/** A shared, input-free keyword panel anchored beside the currently hovered card. */
public final class CardKeywordTooltip extends Table {
  private static final float TEXT_WIDTH = 276f;
  private static final float PADDING = 12f;
  private static final float GAP = 12f;

  private final Label.LabelStyle titleStyle;
  private final Label.LabelStyle bodyStyle;
  private final Vector2 corner = new Vector2();
  private Actor anchor;

  /** Borrows fonts and drawables from the screen-owned skin. */
  public CardKeywordTooltip(Skin skin) {
    titleStyle = new Label.LabelStyle(skin.get("small", Label.LabelStyle.class));
    titleStyle.fontColor = new Color(Color.GOLD);
    bodyStyle = new Label.LabelStyle(skin.get("small", Label.LabelStyle.class));
    bodyStyle.fontColor = new Color(Color.WHITE);
    setBackground(skin.newDrawable("white", new Color(0.04f, 0.06f, 0.09f, 0.98f)));
    pad(PADDING);
    defaults().width(TEXT_WIDTH).left();
    setTouchable(Touchable.disabled);
    setVisible(false);
  }

  /** Adds hover behaviour to the outer card actor without consuming its gameplay input. */
  public void attachTo(Actor card, String description) {
    List<CardKeyword> keywords = CardKeyword.findIn(description);
    if (keywords.isEmpty()) {
      return;
    }
    card.addListener(
        new InputListener() {
          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            if (pointer == -1
                && !Gdx.input.isButtonPressed(Input.Buttons.LEFT)
                && (fromActor == null || !fromActor.isDescendantOf(card))) {
              showFor(card, keywords);
            }
          }

          @Override
          public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
            if (pointer == -1 && (toActor == null || !toActor.isDescendantOf(card))) {
              hideFor(card);
            }
          }

          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            hideFor(card);
            return false;
          }
        });
  }

  private void showFor(Actor card, List<CardKeyword> keywords) {
    Stage stage = card.getStage();
    if (stage == null) {
      return;
    }
    clearChildren();
    for (CardKeyword keyword : keywords) {
      add(new Label(keyword.title(), titleStyle)).padTop(getChildren().size == 0 ? 0f : 10f);
      row();
      Label body = new Label(keyword.description(), bodyStyle);
      body.setWrap(true);
      add(body).padTop(4f);
      row();
    }
    pack();
    anchor = card;
    setVisible(true);
    stage.addActor(this);
    updatePosition();
  }

  private void hideFor(Actor card) {
    if (anchor == card) {
      hide();
    }
  }

  /** Removes the panel when the hand refreshes or its screen closes. */
  public void hide() {
    anchor = null;
    setVisible(false);
    remove();
  }

  @Override
  public void act(float delta) {
    super.act(delta);
    if (anchor == null) {
      return;
    }
    if (anchor.getStage() != getStage()
        || !anchor.isVisible()
        || anchor.getTouchable() == Touchable.disabled) {
      hide();
      return;
    }
    updatePosition();
  }

  private void updatePosition() {
    float left = Float.POSITIVE_INFINITY;
    float right = Float.NEGATIVE_INFINITY;
    float top = Float.NEGATIVE_INFINITY;
    for (int x = 0; x <= 1; x++) {
      for (int y = 0; y <= 1; y++) {
        corner.set(x * anchor.getWidth(), y * anchor.getHeight());
        anchor.localToStageCoordinates(corner);
        left = Math.min(left, corner.x);
        right = Math.max(right, corner.x);
        top = Math.max(top, corner.y);
      }
    }
    Stage stage = getStage();
    float panelX = right + GAP;
    if (panelX + getWidth() > stage.getWidth()) {
      panelX = left - GAP - getWidth();
    }
    setPosition(
        MathUtils.clamp(panelX, 0f, Math.max(0f, stage.getWidth() - getWidth())),
        MathUtils.clamp(top - getHeight(), 0f, Math.max(0f, stage.getHeight() - getHeight())));
  }
}

package com.csse3200.game.components.cards;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.cards.runtime.ResolvedCard;

/** Enlarged read-only card and complete rules over a translucent battle backdrop. */
final class CardInspectionPanel extends Table {
  private final CardWidget face;
  private final Label title;
  private final Label metadata;
  private final Label rules;
  private final Table content = new Table();
  private final Table details = new Table();
  private final ScrollPane scroll;
  private final TextButton close;
  private float layoutWidth = -1f;
  private float layoutHeight = -1f;

  CardInspectionPanel(Skin skin, CardWidgetAssets assets, ResolvedCard card, Runnable onClose) {
    setName("card-inspection-overlay");
    setFillParent(true);
    setTouchable(Touchable.enabled);
    setBackground(skin.newDrawable("white", new Color(0f, 0f, 0f, 0.68f)));
    // Consume backdrop clicks without dismissing or activating the hand below it.
    addListener(
        new InputListener() {
          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            return true;
          }
        });

    face = new CardWidget(card, assets);
    title = new Label("", textStyle(skin, Color.WHITE));
    title.setWrap(true);
    metadata = new Label("", textStyle(skin, new Color(0.9f, 0.85f, 0.72f, 1f)));
    metadata.setWrap(true);
    rules = new Label("", textStyle(skin, Color.WHITE));
    rules.setWrap(true);
    rules.setAlignment(Align.topLeft);
    details.top().left();
    details.add(title).growX().minWidth(0f).padBottom(16f);
    details.row();
    details.add(metadata).growX().minWidth(0f).padBottom(20f);
    details.row();
    details.add(rules).growX().minWidth(0f);
    scroll = new ScrollPane(details, skin);
    scroll.setScrollingDisabled(true, false);
    scroll.setFadeScrollBars(false);

    close = new TextButton("Close", skin);
    close.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            onClose.run();
          }
        });
    add(content);
    setCard(card);
  }

  void setCard(ResolvedCard card) {
    face.setCard(card);
    title.setText(card.name());
    metadata.setText(
        "Cost: "
            + card.cost()
            + "  |  "
            + FramedCardFace.formatType(card.type())
            + "\n"
            + FramedCardFace.formatTarget(card.target())
            + (card.upgraded() ? "\nUpgraded" : ""));
    rules.setText(card.description());
    scroll.setScrollY(0f);
  }

  Actor closeControl() {
    return close;
  }

  Actor scrollControl() {
    return scroll;
  }

  CardWidget face() {
    return face;
  }

  private static Label.LabelStyle textStyle(Skin skin, Color colour) {
    Label.LabelStyle style = new Label.LabelStyle(skin.get("default", Label.LabelStyle.class));
    style.fontColor = colour;
    return style;
  }

  @Override
  public void layout() {
    // Reparenting controls on every validation would drop keyboard/scroll focus mid-inspection.
    if (layoutWidth == getWidth() && layoutHeight == getHeight()) {
      super.layout();
      return;
    }
    layoutWidth = getWidth();
    layoutHeight = getHeight();
    float availableWidth = Math.max(1f, getWidth() - 48f);
    float availableHeight = Math.max(1f, getHeight() - 100f);
    boolean sideBySide = availableWidth >= 600f;
    float scale =
        Math.min(
            1.5f,
            Math.min(
                Math.max(1f, availableHeight - (sideBySide ? 0f : 136f)) / CardWidget.CARD_HEIGHT,
                (sideBySide ? availableWidth * 0.48f : availableWidth) / CardWidget.CARD_WIDTH));
    float width = CardWidget.CARD_WIDTH * scale;
    float height = CardWidget.CARD_HEIGHT * scale;
    Actor keyboardFocus = getStage() == null ? null : getStage().getKeyboardFocus();
    Actor scrollFocus = getStage() == null ? null : getStage().getScrollFocus();
    content.clearChildren();
    content.add(face).minSize(0f).size(width, height);
    if (sideBySide) {
      content
          .add(scroll)
          .minSize(0f)
          .width(Math.min(360f, availableWidth - width - 32f))
          .height(height)
          .padLeft(32f);
    } else {
      // Small allocations retain complete text in the scrollable card/details column.
      content.row();
      content.add(scroll).minSize(0f).width(width).height(120f).padTop(16f);
    }
    content.row();
    content.add(close).colspan(sideBySide ? 2 : 1).height(44f).padTop(16f);
    super.layout();
    if (keyboardFocus != null && keyboardFocus.isDescendantOf(this)) {
      getStage().setKeyboardFocus(keyboardFocus);
    }
    if (scrollFocus != null && scrollFocus.isDescendantOf(this)) {
      getStage().setScrollFocus(scrollFocus);
    }
  }
}

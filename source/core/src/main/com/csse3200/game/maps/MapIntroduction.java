package com.csse3200.game.maps;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;

/** A collapsible introduction in the map's left margin, leaving map input available. */
public final class MapIntroduction extends Group {
  private static final float PANEL_WIDTH = 248f;
  private static final float MARGIN = 16f;
  private static final float HUD_GAP = 124f;
  private final Table panel;
  private final TextButton helpButton;

  /** Borrows the map's skin and consumes only the current run's first introduction. */
  public MapIntroduction(Skin skin, RunState runState) {
    setName("map-introduction");
    setTouchable(Touchable.childrenOnly);

    panel = new Table();
    panel.setName("map-introduction-panel");
    panel.setBackground(skin.newDrawable("window", Color.valueOf("514031")));
    panel.setTouchable(Touchable.enabled);
    panel.pad(0f, 18f, 18f, 18f);
    panel.defaults().width(PANEL_WIDTH - 36f).left();

    Label.LabelStyle headingStyle = new Label.LabelStyle(skin.get("large", Label.LabelStyle.class));
    headingStyle.fontColor = Color.valueOf("EFC26C");
    Label heading = new Label("Your Journey", headingStyle);
    heading.setFontScale(0.85f);
    heading.setAlignment(Align.center);
    panel.add(heading).height(32f).padBottom(16f);
    panel.row();

    Label body =
        new Label(
            "Reach the Boss at the top of the map.\n\n"
                + "Start by selecting an available node connected to the starting point.\n\n"
                + "Complete its encounter, then choose your next connected node. "
                + "At each fork, follow one path. You don't need to explore every branch.",
            skin,
            "popup");
    body.setName("map-introduction-text");
    body.setWrap(true);
    panel.add(body);
    panel.row();

    TextButton closeButton = new TextButton("Got it", skin, "popup");
    closeButton.setName("map-introduction-close");
    closeButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            setExpanded(false);
          }
        });
    panel.add(closeButton).height(42f).padTop(18f);
    panel.pack();
    addActor(panel);

    helpButton = new TextButton("?", skin, "popup");
    helpButton.setName("map-introduction-help");
    helpButton.setSize(42f, 42f);
    helpButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            setExpanded(true);
          }
        });
    addActor(helpButton);
    setExpanded(runState.consumeMapIntroduction());
  }

  private void setExpanded(boolean expanded) {
    panel.setVisible(expanded);
    helpButton.setVisible(!expanded);
  }

  /** Keeps the panel below the stats bar and outside the map's node area. */
  public void resize(float width, float height) {
    setSize(width, height);
    panel.setPosition(MARGIN, height - HUD_GAP - panel.getHeight());
    helpButton.setPosition(MARGIN, height - HUD_GAP - helpButton.getHeight());
  }
}

package com.csse3200.game.maps;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.spritedisplay.clickable.BattleMenuSkins;

/** A collapsible introduction in the map's left margin, leaving map input available. */
public final class MapIntroduction extends Group {
  private static final float PANEL_WIDTH = 248f;
  private static final float MARGIN = 16f;
  private static final float HUD_GAP = 124f;
  private final Table panel;
  private final TextButton helpButton;

  /** Borrows the map's skin and panel texture, consuming this run's first introduction. */
  public MapIntroduction(Skin skin, RunState runState, Texture panelTexture) {
    setName("map-introduction");
    setTouchable(Touchable.childrenOnly);

    panel = new Table();
    panel.setName("map-introduction-panel");
    panelTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    NinePatch frame = new NinePatch(panelTexture, 32, 32, 32, 32);
    frame.scale(0.5f, 0.5f);
    NinePatchDrawable background = new NinePatchDrawable(frame);
    background.setMinWidth(0f);
    background.setMinHeight(0f);
    panel.setBackground(background);
    panel.setTouchable(Touchable.enabled);
    panel.pad(18f);
    panel.defaults().width(PANEL_WIDTH - 36f).left();

    Label.LabelStyle headingStyle = new Label.LabelStyle(skin.get("large", Label.LabelStyle.class));
    headingStyle.fontColor = Color.valueOf("D49C45");
    Label heading = new Label("Your Journey", headingStyle);
    heading.setFontScale(0.85f);
    heading.setAlignment(Align.center);
    panel.add(heading).height(32f).padBottom(16f);
    panel.row();

    Label body =
        new Label(
            """
            Reach the Boss at the top of the map.

            Start by selecting an available node connected to the starting point.

            Complete its encounter, then choose your next connected node. \
            At each fork, follow one path. You don't need to explore every branch.\
            """,
            skin,
            "popup");
    body.setName("map-introduction-text");
    body.setWrap(true);
    panel.add(body);
    panel.row();

    TextButton closeButton =
        new TextButton("Got it", BattleMenuSkins.frameStyle(skin.getFont("button")));
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

    helpButton = new TextButton("?", BattleMenuSkins.frameStyle(skin.getFont("button")));
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

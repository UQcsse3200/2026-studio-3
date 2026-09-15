package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.GdxGame;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.RunState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stand-in encounter that stands in for combat (Team 3) and event/shop (Team 2) until those can be
 * entered from a map node. Lets the whole map to encounter to map loop be played and tested in the
 * meantime.
 *
 * <p>Screens aren't passed any arguments, so which node this belongs to is read from the run state.
 * Finishing reports back through onEncounterComplete and returns to the same map.
 */
public class EncounterScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(EncounterScreen.class);

  private static final Color BACKGROUND = new Color(248f / 255f, 249f / 255f, 178f / 255f, 1f);
  private static final float BUTTON_WIDTH = 200f;
  private static final float BUTTON_HEIGHT = 60f;

  private final GdxGame game;
  private final RunState runState;
  private final Stage stage;
  private final Skin skin;

  public EncounterScreen(GdxGame game) {
    this.game = game;
    this.runState = game.getRunState();
    this.stage = new Stage(new ScreenViewport());
    this.skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));

    buildUi();
    Gdx.input.setInputProcessor(stage);
  }

  private void buildUi() {
    float x = Gdx.graphics.getWidth() / 2f - BUTTON_WIDTH / 2f;
    float y = Gdx.graphics.getHeight() / 2f;

    Label title = new Label(describeRoom(), skin);
    title.setPosition(x, y + 120f);
    stage.addActor(title);

    addButton("Complete encounter", x, y + 40f, true);
    addButton("Fail encounter", x, y - 40f, false);
  }

  private void addButton(String text, float x, float y, boolean success) {
    TextButton button = new TextButton(text, skin);
    button.setBounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT);

    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            finish(success);
          }
        });

    stage.addActor(button);
  }

  /** Names the room being entered. Swapped for a handoff to the owning team's screen later. */
  private String describeRoom() {
    Integer nodeId = runState.getActiveNodeId();

    if (nodeId == null || runState.getMapGraph() == null) {
      return "Encounter";
    }

    MapNode node = runState.getMapGraph().getNode(nodeId);
    return node == null ? "Encounter" : node.getRoomType() + " encounter (placeholder)";
  }

  private void finish(boolean success) {
    logger.info("Encounter finished, success={}", success);
    runState.completeEncounter(success);

    // A failed encounter unlocks nothing, so there would be no room left to pick. The run is over.
    if (!success) {
      runState.endRun();
      game.setScreen(GdxGame.ScreenType.MAIN_MENU);
      return;
    }

    game.setScreen(GdxGame.ScreenType.MAP);
  }

  @Override
  public void render(float delta) {
    ScreenUtils.clear(BACKGROUND);
    stage.act(delta);
    stage.draw();
  }

  @Override
  public void resize(int width, int height) {
    stage.getViewport().update(width, height, true);
  }

  @Override
  public void dispose() {
    Gdx.input.setInputProcessor(null);
    stage.dispose();
    skin.dispose();
  }
}

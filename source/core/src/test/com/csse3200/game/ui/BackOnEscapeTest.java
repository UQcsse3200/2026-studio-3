package com.csse3200.game.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.GdxGame;
import com.csse3200.game.GdxGame.ScreenType;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.screens.SettingsScreen;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;

@ExtendWith(GameExtension.class)
class BackOnEscapeTest {
  private final AtomicInteger backPresses = new AtomicInteger();
  private Stage stage;
  private Skin skin;

  @BeforeEach
  void setUp() {
    stage = new Stage(new FitViewport(1280, 800), mock(Batch.class));
    skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    RenderService render = new RenderService();
    render.setStage(stage);
    ServiceLocator.registerRenderService(render);
  }

  @AfterEach
  void tearDown() {
    stage.dispose();
    skin.dispose();
  }

  @Test
  void escapePressesTheBackButton() {
    stage.addActor(inATable(backButton()));
    BackOnEscape.attach(ScreenType.SETTINGS);

    stage.keyDown(Keys.ESCAPE);

    assertEquals(1, backPresses.get());
  }

  @Test
  void otherKeysDoNothing() {
    stage.addActor(backButton());
    BackOnEscape.attach(ScreenType.CARD_LIBRARY);

    stage.keyDown(Keys.ENTER);

    assertEquals(0, backPresses.get());
  }

  @Test
  void hiddenBackButtonsAreSkipped() {
    Table hiddenPanel = inATable(backButton());
    hiddenPanel.setVisible(false);
    stage.addActor(hiddenPanel);
    BackOnEscape.attach(ScreenType.SAVE_LOAD);

    stage.keyDown(Keys.ESCAPE);

    assertEquals(0, backPresses.get());
  }

  /** An open dropdown closes on Escape, and that shouldn't also leave the screen. */
  @Test
  void somethingThatUsesEscapeItselfGetsItFirst() {
    stage.addActor(backButton());
    Actor dropdown = new Actor();
    dropdown.addListener(
        new InputListener() {
          @Override
          public boolean keyDown(InputEvent event, int keycode) {
            return keycode == Keys.ESCAPE;
          }
        });
    stage.addActor(dropdown);
    stage.setKeyboardFocus(dropdown);
    BackOnEscape.attach(ScreenType.SETTINGS);

    stage.keyDown(Keys.ESCAPE);

    assertEquals(0, backPresses.get());
  }

  @Test
  void screensWithoutABackButtonAreLeftAlone() {
    BackOnEscape.attach(ScreenType.MAIN_MENU);
    BackOnEscape.attach(ScreenType.MAP);

    assertEquals(0, stage.getRoot().getListeners().size);
  }

  /** GdxGame builds the new screen before this runs, so the old screen's stage is left alone. */
  @Test
  void gdxGameAddsItToTheScreenItJustBuilt() {
    Stage oldStage = new Stage(new FitViewport(1280, 800), mock(Batch.class));
    RenderService oldScreen = new RenderService();
    oldScreen.setStage(oldStage);
    ServiceLocator.registerRenderService(oldScreen);
    RenderService newScreen = new RenderService();
    newScreen.setStage(stage);
    stage.addActor(backButton());

    try (MockedConstruction<SettingsScreen> screens =
        mockConstruction(
            SettingsScreen.class,
            (screen, context) -> ServiceLocator.registerRenderService(newScreen))) {
      new GdxGame().setScreen(ScreenType.SETTINGS);
      assertEquals(1, screens.constructed().size());
    }
    stage.keyDown(Keys.ESCAPE);

    assertEquals(1, backPresses.get());
    assertEquals(0, oldStage.getRoot().getListeners().size);
    oldStage.dispose();
  }

  private TextButton backButton() {
    TextButton back = new TextButton("Back", skin);
    back.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            backPresses.incrementAndGet();
          }
        });
    return back;
  }

  private static Table inATable(Actor actor) {
    Table table = new Table();
    table.add(actor);
    return table;
  }
}

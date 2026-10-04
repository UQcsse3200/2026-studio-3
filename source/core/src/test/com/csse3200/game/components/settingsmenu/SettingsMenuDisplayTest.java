package com.csse3200.game.components.settingsmenu;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.UserSettings;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

@ExtendWith(GameExtension.class)
class SettingsMenuDisplayTest {
  private Graphics originalGraphics;
  private Stage stage;
  private Entity entity;
  private MockedStatic<UserSettings> settingsMock;
  private UserSettings.Settings settings;
  private TextField fpsField;
  private CheckBox fullscreen;
  private TextButton applyButton;

  @BeforeEach
  void setUp() {
    originalGraphics = Gdx.graphics;
    Gdx.graphics = mock(Graphics.class);
    Graphics.DisplayMode mode = mock(Graphics.DisplayMode.class);
    when(Gdx.graphics.getDisplayMode()).thenReturn(mode);
    when(Gdx.graphics.getDisplayModes(Gdx.graphics.getMonitor()))
        .thenReturn(new Graphics.DisplayMode[] {mode});

    settings = new UserSettings.Settings();
    settingsMock = mockStatic(UserSettings.class);
    settingsMock.when(UserSettings::get).thenAnswer(invocation -> settings);

    stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    EntityService entityService = new EntityService();
    ServiceLocator.registerEntityService(entityService);
    entity = new Entity().addComponent(new SettingsMenuDisplay(mock(GdxGame.class)));
    entityService.register(entity);

    fpsField = findActor(stage.getRoot(), TextField.class, actor -> true);
    fullscreen = findActor(stage.getRoot(), CheckBox.class, actor -> true);
    applyButton =
        findActor(
            stage.getRoot(),
            TextButton.class,
            button -> button.getText().toString().equals("Apply"));
    assertNotNull(fpsField);
    assertNotNull(fullscreen);
    assertNotNull(applyButton);
  }

  @AfterEach
  void tearDown() {
    // Run every cleanup step even if setup was incomplete or another cleanup step fails.
    assertAll(
        () -> {
          if (entity != null) {
            entity.dispose();
          }
        },
        () -> {
          if (stage != null) {
            stage.dispose();
          }
        },
        () -> {
          if (settingsMock != null) {
            settingsMock.close();
          }
        },
        () -> Gdx.graphics = originalGraphics);
  }

  @Test
  void invalidFpsBlocksAllSettingsChangesAndPreservesInput() {
    fullscreen.setChecked(true);
    for (String input : new String[] {"-60", "0", "abc", "", "60.5", "2147483648"}) {
      fpsField.setText(input);
      applyButton.fire(new ChangeListener.ChangeEvent());

      assertEquals(input, fpsField.getText());
      assertEquals(60, settings.fps);
      assertFalse(settings.fullscreen);
    }
    settingsMock.verify(
        () -> UserSettings.set(any(UserSettings.Settings.class), eq(true)), never());
  }

  @Test
  void correctingInvalidFpsAppliesAllEditsOnce() {
    fullscreen.setChecked(true);
    fpsField.setText("-60");
    applyButton.fire(new ChangeListener.ChangeEvent());

    fpsField.setText(" 120 ");
    applyButton.fire(new ChangeListener.ChangeEvent());

    assertEquals(120, settings.fps);
    assertTrue(settings.fullscreen);
    settingsMock.verify(() -> UserSettings.set(settings, true));
  }

  private static <T extends Actor> T findActor(Group group, Class<T> type, Predicate<T> predicate) {
    for (Actor actor : group.getChildren()) {
      if (type.isInstance(actor) && predicate.test(type.cast(actor))) {
        return type.cast(actor);
      }
      if (actor instanceof Group child) {
        T match = findActor(child, type, predicate);
        if (match != null) {
          return match;
        }
      }
    }
    return null;
  }
}

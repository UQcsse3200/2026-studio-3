package com.csse3200.game.components.mainmenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class MainMenuDisplayTest {
  private Stage stage;
  private Entity menu;
  private MainMenuDisplay display;
  private ResourceService resourceService;
  private boolean menuDisposed;

  @BeforeEach
  void setUp() {
    stage = new Stage(new FitViewport(1280f, 800f), mock(Batch.class));
    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    EntityService entityService = new EntityService();
    ServiceLocator.registerEntityService(entityService);

    resourceService = mock(ResourceService.class);
    Texture backgroundTexture = texture(1586, 992);
    Texture buttonFrameTexture = texture(2172, 724);
    Texture titleLogoTexture = texture(2172, 724);
    when(resourceService.getAsset(MainMenuDisplay.BACKGROUND_TEXTURE, Texture.class))
        .thenReturn(backgroundTexture);
    when(resourceService.getAsset(MainMenuDisplay.BUTTON_FRAME_TEXTURE, Texture.class))
        .thenReturn(buttonFrameTexture);
    when(resourceService.getAsset(MainMenuDisplay.TITLE_LOGO_TEXTURE, Texture.class))
        .thenReturn(titleLogoTexture);
    ServiceLocator.registerResourceService(resourceService);

    display = new MainMenuDisplay();
    menu = new Entity().addComponent(display);
    entityService.register(menu);
  }

  @AfterEach
  void tearDown() {
    if (!menuDisposed) {
      menu.dispose();
    }
    stage.dispose();
  }

  @Test
  void buildsPlayerFacingMenuInOrder() {
    assertEquals(
        List.of("New Game", "Load Game", "Library", "Settings", "Exit"),
        display.getMenuButtons().stream().map(button -> button.getText().toString()).toList());
    assertEquals(
        List.of("start", "load", "bestiary", "settings", "exit"),
        display.getMenuButtons().stream().map(TextButton::getName).toList());
    assertInstanceOf(Image.class, display.getRootStack().getChild(0));
    verify(resourceService).getAsset(MainMenuDisplay.BACKGROUND_TEXTURE, Texture.class);
    verify(resourceService).getAsset(MainMenuDisplay.TITLE_LOGO_TEXTURE, Texture.class);
    verify(resourceService).getAsset(MainMenuDisplay.BUTTON_FRAME_TEXTURE, Texture.class);
  }

  @Test
  void buttonsEmitTheirNamedEvents() {
    List<String> events =
        List.of(
            MainMenuDisplay.START_EVENT,
            MainMenuDisplay.LOAD_EVENT,
            MainMenuDisplay.BESTIARY_EVENT,
            MainMenuDisplay.SETTINGS_EVENT,
            MainMenuDisplay.EXIT_EVENT);

    for (int i = 0; i < events.size(); i++) {
      AtomicInteger eventCount = new AtomicInteger();
      menu.getEvents().addListener(events.get(i), eventCount::incrementAndGet);
      display.getMenuButtons().get(i).fire(new ChangeEvent());
      assertEquals(1, eventCount.get());
    }
  }

  @Test
  void buildsTemporaryDemoShortcuts() {
    assertEquals(
        List.of("Demo Shop", "Demo Event"),
        display.getDemoButtons().stream().map(button -> button.getText().toString()).toList());
    assertEquals(
        List.of(MainMenuDisplay.DEMO_SHOP_EVENT, MainMenuDisplay.DEMO_EVENT_EVENT),
        display.getDemoButtons().stream().map(TextButton::getName).toList());
  }

  @Test
  void demoButtonsEmitTheirNamedEvents() {
    List<String> events =
        List.of(MainMenuDisplay.DEMO_SHOP_EVENT, MainMenuDisplay.DEMO_EVENT_EVENT);

    for (int i = 0; i < events.size(); i++) {
      AtomicInteger eventCount = new AtomicInteger();
      menu.getEvents().addListener(events.get(i), eventCount::incrementAndGet);
      display.getDemoButtons().get(i).fire(new ChangeEvent());
      assertEquals(1, eventCount.get());
    }
  }

  @Test
  void layoutKeepsVirtualSizeAcrossWindowShapes() {
    stage.getViewport().update(1920, 1080, true);
    display.getRootStack().validate();
    assertEquals(1280f, display.getRootStack().getWidth());
    assertEquals(800f, display.getRootStack().getHeight());

    stage.getViewport().update(900, 1200, true);
    display.getRootStack().validate();
    assertEquals(1280f, display.getRootStack().getWidth());
    assertEquals(800f, display.getRootStack().getHeight());
  }

  @Test
  void disposalRemovesMenuActors() {
    menu.dispose();
    menuDisposed = true;

    assertNull(display.getRootStack().getParent());
    assertEquals(0, display.getRootStack().getChildren().size);
  }

  private static Texture texture(int width, int height) {
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(width);
    when(texture.getHeight()).thenReturn(height);
    return texture;
  }
}

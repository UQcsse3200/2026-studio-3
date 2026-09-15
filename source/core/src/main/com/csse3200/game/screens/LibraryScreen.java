package com.csse3200.game.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.GdxGame;
import com.csse3200.game.components.library.LibraryMenuDisplay;
import com.csse3200.game.components.mainmenu.MainMenuDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Menu screen for choosing between card and enemy library views. */
public class LibraryScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(LibraryScreen.class);
  private static final String[] LIBRARY_TEXTURES = {
    MainMenuDisplay.BACKGROUND_TEXTURE, MainMenuDisplay.BUTTON_FRAME_TEXTURE
  };

  private final GdxGame game;
  private final Renderer renderer;

  public LibraryScreen(GdxGame game) {
    this.game = game;

    logger.debug("Initialising library screen services");
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());
    ServiceLocator.registerTimeSource(new GameTime());

    renderer = RenderFactory.createRenderer();
    loadAssets();
    createUI();
  }

  private void loadAssets() {
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(LIBRARY_TEXTURES);
    resourceService.loadAll();
  }

  private void createUI() {
    Stage stage = ServiceLocator.getRenderService().getStage();
    Entity ui = new Entity();
    ui.addComponent(new InputDecorator(stage, 10)).addComponent(new LibraryMenuDisplay(game));
    ServiceLocator.getEntityService().register(ui);
  }

  @Override
  public void render(float delta) {
    ServiceLocator.getEntityService().update();
    renderer.render();
  }

  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
  }

  @Override
  public void dispose() {
    logger.debug("Disposing library screen");
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getResourceService().unloadAssets(LIBRARY_TEXTURES);
    ServiceLocator.getResourceService().dispose();
    ServiceLocator.clear();
  }
}

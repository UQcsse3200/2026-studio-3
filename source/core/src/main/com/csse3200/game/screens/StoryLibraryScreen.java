package com.csse3200.game.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.GdxGame;
import com.csse3200.game.components.library.CardLibraryDisplay;
import com.csse3200.game.components.library.StoryLibraryDisplay;
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

/** Library screen where players read the story and setting (Sprint 4, #431). */
public class StoryLibraryScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(StoryLibraryScreen.class);
  static final String[] STORY_LIBRARY_TEXTURES = {
    MainMenuDisplay.BACKGROUND_TEXTURE, CardLibraryDisplay.BUTTON_TEXTURE
  };

  private final Renderer renderer;

  public StoryLibraryScreen(GdxGame game) {
    logger.debug("Initialising story library screen services");
    // Uses the game's shared ResourceService, like CardLibraryScreen, so returning to the
    // Library menu still has a resource service to load from.
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());
    ServiceLocator.registerTimeSource(new GameTime());

    renderer = RenderFactory.createRenderer();
    loadAssets();
    createUI(game);
  }

  private void loadAssets() {
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(STORY_LIBRARY_TEXTURES);
    resourceService.loadAll();
  }

  private void createUI(GdxGame game) {
    Stage stage = ServiceLocator.getRenderService().getStage();
    Entity ui = new Entity();
    ui.addComponent(new InputDecorator(stage, 10)).addComponent(new StoryLibraryDisplay(game));
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
    logger.debug("Disposing story library screen");
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getResourceService().unloadAssets(STORY_LIBRARY_TEXTURES);
  }
}

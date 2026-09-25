package com.csse3200.game.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.GdxGame;
import com.csse3200.game.components.library.ItemLibraryDisplay;
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

/**
 * Displays a read-only catalogue of all item types in {@link com.csse3200.game.rewards.ItemType}.
 */
public class ItemLibraryScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(ItemLibraryScreen.class);
  private static final String[] ITEM_LIBRARY_TEXTURES = {
    MainMenuDisplay.BACKGROUND_TEXTURE,
    MainMenuDisplay.BUTTON_FRAME_TEXTURE,
    "images/ui/energy-crystal.png",
    "images/ui/iron-aegis.png",
    "images/ui/lucky-coin.png",
    "images/ui/merchants-favor.png",
    "images/ui/warriors-crest.png"
  };

  private final GdxGame game;
  private final Renderer renderer;

  public ItemLibraryScreen(GdxGame game) {
    this.game = game;

    logger.debug("Initialising item library screen services");
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
    resourceService.loadTextures(ITEM_LIBRARY_TEXTURES);
    resourceService.loadAll();
  }

  private void createUI() {
    Stage stage = ServiceLocator.getRenderService().getStage();
    Entity ui = new Entity();
    ui.addComponent(new InputDecorator(stage, 10)).addComponent(new ItemLibraryDisplay(game));
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
    logger.debug("Disposing item library screen");
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getResourceService().unloadAssets(ITEM_LIBRARY_TEXTURES);
    ServiceLocator.getResourceService().dispose();
    ServiceLocator.clear();
  }
}

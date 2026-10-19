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
import com.csse3200.game.rewards.ItemDiscoveryService;
import com.csse3200.game.rewards.ItemType;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Displays a read-only catalogue of all item types in {@link com.csse3200.game.rewards.ItemType}.
 */
public class ItemLibraryScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(ItemLibraryScreen.class);

  static String[] collectTexturePaths() {
    return collectTexturePaths(null);
  }

  static String[] collectTexturePaths(ItemDiscoveryService itemDiscoveryService) {
    Set<String> paths = new LinkedHashSet<>();
    paths.add(MainMenuDisplay.BACKGROUND_TEXTURE);
    paths.add(MainMenuDisplay.BUTTON_FRAME_TEXTURE);
    paths.add(ItemLibraryDisplay.BUTTON_TEXTURE);
    for (ItemType item : ItemType.values()) {
      if (itemDiscoveryService == null || itemDiscoveryService.isDiscovered(item)) {
        paths.add(ItemLibraryDisplay.resolveArtworkPath(item));
      }
    }
    return paths.toArray(new String[0]);
  }

  private final GdxGame game;
  private final Renderer renderer;
  private final String[] itemLibraryTextures;

  public ItemLibraryScreen(GdxGame game) {
    this.game = game;
    itemLibraryTextures = collectTexturePaths(game.getItemDiscoveryService());

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
    resourceService.loadTextures(itemLibraryTextures);
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
    ServiceLocator.getResourceService().unloadAssets(itemLibraryTextures);
    ServiceLocator.getResourceService().dispose();
    ServiceLocator.clear();
  }
}

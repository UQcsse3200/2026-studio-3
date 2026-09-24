package com.csse3200.game.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.CardLoadingException;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.components.library.CardLibraryDisplay;
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
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Displays card definitions loaded from Team 6 card configuration data. */
public class CardLibraryScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(CardLibraryScreen.class);

  private final GdxGame game;
  private final Renderer renderer;
  private final CardDiscoveryService discovery;
  private final String[] cardLibraryTextures;

  public CardLibraryScreen(GdxGame game) {
    this.game = game;
    this.discovery = game.getCardDiscoveryService();
    this.cardLibraryTextures = collectTexturePaths();

    logger.debug("Initialising card library screen services");
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
    resourceService.loadTextures(cardLibraryTextures);
    resourceService.loadAll();
  }

  private void createUI() {
    Stage stage = ServiceLocator.getRenderService().getStage();
    Entity ui = new Entity();
    ui.addComponent(new InputDecorator(stage, 10))
        .addComponent(new CardLibraryDisplay(game, discovery));
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
    logger.debug("Disposing card library screen");
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getResourceService().unloadAssets(cardLibraryTextures);
    ServiceLocator.getResourceService().dispose();
    ServiceLocator.clear();
  }

  static String[] collectTexturePaths() {
    Set<String> paths = new LinkedHashSet<>();
    paths.add(MainMenuDisplay.BACKGROUND_TEXTURE);
    paths.add(MainMenuDisplay.BUTTON_FRAME_TEXTURE);
    try {
      for (CardConfig card : CardConfigLoader.loadCards()) {
        if (card.texturePath != null && !card.texturePath.isBlank()) {
          paths.add(card.texturePath);
        }
      }
    } catch (CardLoadingException exception) {
      logger.warn("Card library artwork preload skipped because cards could not load", exception);
    }
    return paths.toArray(new String[0]);
  }
}

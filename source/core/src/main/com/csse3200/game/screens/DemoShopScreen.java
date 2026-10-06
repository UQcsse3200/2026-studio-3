package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.GdxGame;
import com.csse3200.game.areas.EncounterGameArea;
import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.components.cards.CardWidgetAssets;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.shop.ShopEncounter;
import java.util.List;

/**
 * Temporary map-free host for the real Shop UI and transaction flow.
 *
 * <p>The preview owns an isolated run state and returns to the main menu when the player leaves.
 * Removing this class and the Demo Shop main-menu hooks removes the shortcut without changing map
 * encounters.
 */
public final class DemoShopScreen extends ScreenAdapter {
  private static final Vector2 CAMERA_POSITION = new Vector2(7.5f, 7.5f);
  private static final int DEMO_GOLD = 200;

  private final GdxGame game;
  private final Renderer renderer;
  private final PhysicsEngine physicsEngine;
  private final EncounterGameArea encounterGameArea;
  private final String[] cardTexturePaths;
  private boolean completionQueued;

  public DemoShopScreen(GdxGame game) {
    this.game = game;

    ServiceLocator.registerTimeSource(new GameTime());
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());

    List<CardConfig> cards = CardConfigLoader.loadCards();
    CardLibrary cardLibrary = new CardLibrary(cards);
    ServiceLocator.registerCardLibrary(cardLibrary);
    cardTexturePaths = CardWidgetAssets.collectTexturePaths(cards);

    ResourceService resources = ServiceLocator.getResourceService();
    resources.loadTextures(cardTexturePaths);
    resources.loadAll();

    PhysicsService physicsService = new PhysicsService();
    ServiceLocator.registerPhysicsService(physicsService);
    physicsEngine = physicsService.getPhysics();

    renderer = RenderFactory.createRenderer();
    renderer.getCamera().getEntity().setPosition(CAMERA_POSITION);
    Entity input = new Entity();
    input.addComponent(new InputDecorator(ServiceLocator.getRenderService().getStage(), 10));
    ServiceLocator.getEntityService().register(input);

    RunState demoRunState = new RunState();
    PlayerRunState demoPlayerState = demoRunState.getOrCreatePlayerState();
    demoPlayerState.restore(
        demoPlayerState.getCurrentHealth(), demoPlayerState.getMaxHealth(), DEMO_GOLD);
    PlayerDeck demoPlayerDeck = demoRunState.getOrCreatePlayerDeck(cardLibrary);

    encounterGameArea =
        new EncounterGameArea(
            new TerrainFactory(renderer.getCamera()),
            ShopEncounter.DEFAULT_NODE_ID,
            RoomType.SHOP,
            (nodeId, success) -> returnToMainMenu(),
            demoPlayerState,
            demoPlayerDeck,
            demoRunState);
    encounterGameArea.create();
  }

  private void returnToMainMenu() {
    if (completionQueued) {
      return;
    }
    completionQueued = true;
    Gdx.app.postRunnable(() -> game.setScreen(GdxGame.ScreenType.MAIN_MENU));
  }

  @Override
  public void render(float delta) {
    physicsEngine.update();
    ServiceLocator.getEntityService().update();
    renderer.render();
  }

  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
  }

  @Override
  public void dispose() {
    encounterGameArea.dispose();
    renderer.dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getResourceService().unloadAssets(cardTexturePaths);
    ServiceLocator.getResourceService().dispose();
    ServiceLocator.clear();
  }
}

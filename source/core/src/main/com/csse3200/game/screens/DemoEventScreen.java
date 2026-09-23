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
import java.util.List;

/**
 * Temporary map-free host for the existing Event encounter UI.
 *
 * <p>Remove this class and the Demo Event hooks in GdxGame and the main menu to remove the preview.
 * No map node or real game run state is created or changed here. An isolated temporary run state is
 * used so run-scoped Events such as Card Fusion can be previewed safely.
 */
public class DemoEventScreen extends ScreenAdapter {
  private static final Vector2 CAMERA_POSITION = new Vector2(7.5f, 7.5f);
  private static final int DEMO_NODE_ID = -1;

  private final GdxGame game;
  private final Renderer renderer;
  private final PhysicsEngine physicsEngine;
  private final EncounterGameArea encounterGameArea;
  private final String[] cardTexturePaths;
  private boolean completionQueued;
  private float fusionResultSeconds = -1f;

  public DemoEventScreen(GdxGame game) {
    this(game, null);
  }

  /** Opens one catalogue Event directly when an ID is supplied, or a random Event otherwise. */
  public DemoEventScreen(GdxGame game, String previewEncounterId) {
    this.game = game;

    ServiceLocator.registerTimeSource(new GameTime());
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());

    List<CardConfig> cards = CardConfigLoader.loadCards();
    CardLibrary cardLibrary = new CardLibrary(cards);
    ServiceLocator.registerCardLibrary(cardLibrary);
    cardTexturePaths =
        cards.stream()
            .map(card -> card.texturePath)
            .filter(path -> path != null && !path.isBlank())
            .distinct()
            .toArray(String[]::new);

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
    PlayerDeck demoPlayerDeck = demoRunState.getOrCreatePlayerDeck(cardLibrary);

    encounterGameArea =
        new EncounterGameArea(
            new TerrainFactory(renderer.getCamera()),
            DEMO_NODE_ID,
            RoomType.EVENT,
            (nodeId, success) -> returnToMainMenu(),
            demoPlayerState,
            demoPlayerDeck,
            demoRunState,
            previewEncounterId);
    encounterGameArea.create();
  }

  private void returnToMainMenu() {
    if (completionQueued) {
      return;
    }
    completionQueued = true;
    if (encounterGameArea.getCardFusionEncounterFlow().isPresent()) {
      fusionResultSeconds = 2f;
    } else {
      Gdx.app.postRunnable(() -> game.setScreen(GdxGame.ScreenType.MAIN_MENU));
    }
  }

  @Override
  public void render(float delta) {
    physicsEngine.update();
    ServiceLocator.getEntityService().update();
    renderer.render();
    if (fusionResultSeconds >= 0f) {
      fusionResultSeconds -= delta;
      if (fusionResultSeconds < 0f) {
        game.setScreen(GdxGame.ScreenType.MAIN_MENU);
      }
    }
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

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
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporary, map-free host for the real Shop and Event UIs.
 *
 * <p>This class exists only for the two main-menu Demo shortcuts and can be deleted with them
 * before submission.
 */
public class DemoEncounterScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(DemoEncounterScreen.class);
  private static final Vector2 CAMERA_POSITION = new Vector2(7.5f, 7.5f);
  private static final int DEMO_NODE_ID = -1;
  private final GdxGame game;
  private final Renderer renderer;
  private final PhysicsEngine physicsEngine;
  private final EncounterGameArea encounterGameArea;
  private final String[] cardTexturePaths;
  private boolean completionQueued;

  public DemoEncounterScreen(GdxGame game, RoomType roomType) {
    this.game = game;
    validateRoomType(roomType);
    logger.info("Opening map-free {} demo", roomType);

    ServiceLocator.registerTimeSource(new GameTime());
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());

    List<CardConfig> cards = CardConfigLoader.loadCards();
    ServiceLocator.registerCardLibrary(new CardLibrary(cards));
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
    createInput();

    encounterGameArea =
        new EncounterGameArea(
            new TerrainFactory(renderer.getCamera()),
            DEMO_NODE_ID,
            roomType,
            (nodeId, success) -> returnToMainMenu());
    encounterGameArea.create();
  }

  private static void validateRoomType(RoomType roomType) {
    if (roomType != RoomType.SHOP && roomType != RoomType.EVENT) {
      throw new IllegalArgumentException("Demo screen only supports SHOP and EVENT rooms");
    }
  }

  private void createInput() {
    Entity inputEntity = new Entity();
    inputEntity.addComponent(new InputDecorator(ServiceLocator.getRenderService().getStage(), 10));
    ServiceLocator.getEntityService().register(inputEntity);
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
    logger.debug("Disposing standalone encounter demo");
    encounterGameArea.dispose();
    renderer.dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getResourceService().unloadAssets(cardTexturePaths);
    ServiceLocator.getResourceService().dispose();
    ServiceLocator.clear();
  }
}

package com.csse3200.game.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.GdxGame;
import com.csse3200.game.bestiary.BestiaryDataSource;
import com.csse3200.game.bestiary.BestiaryEntry;
import com.csse3200.game.bestiary.MockBestiaryDataSource;
import com.csse3200.game.components.bestiary.BestiaryDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Screen containing the enemy bestiary. */
public class BestiaryScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(BestiaryScreen.class);
  private static final String FALLBACK_IMAGE = "images/enemies/default.png";

  private final GdxGame game;
  private final Renderer renderer;
  private final BestiaryDataSource dataSource;
  private final String[] texturePaths;

  /** Creates a bestiary screen backed by temporary mock entries. */
  public BestiaryScreen(GdxGame game) {
    this.game = game;
    this.dataSource = new MockBestiaryDataSource();
    this.texturePaths = collectTexturePaths(dataSource);

    logger.debug("Initialising bestiary screen services");
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());

    renderer = RenderFactory.createRenderer();
    loadAssets();
    createUI();
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
    renderer.dispose();
    ServiceLocator.getResourceService().unloadAssets(texturePaths);
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.clear();
  }

  private void loadAssets() {
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(texturePaths);
    resourceService.loadAll();
  }

  private void createUI() {
    Stage stage = ServiceLocator.getRenderService().getStage();
    Entity ui = new Entity();
    ui.addComponent(
            new BestiaryDisplay(dataSource, () -> game.setScreen(GdxGame.ScreenType.MAIN_MENU)))
        .addComponent(new InputDecorator(stage, 10));
    ServiceLocator.getEntityService().register(ui);
  }

  static String[] collectTexturePaths(BestiaryDataSource dataSource) {
    Set<String> paths = new LinkedHashSet<>();
    paths.add(FALLBACK_IMAGE);
    for (BestiaryEntry entry : dataSource.getEntries()) {
      if (!entry.getImagePath().isBlank()) {
        paths.add(entry.getImagePath());
      }
    }
    return paths.toArray(new String[0]);
  }
}

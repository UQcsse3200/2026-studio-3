package com.csse3200.game.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.GdxGame;
import com.csse3200.game.bestiary.BestiaryEntryView;
import com.csse3200.game.bestiary.BestiaryService;
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
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Screen containing the enemy bestiary. */
public class BestiaryScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(BestiaryScreen.class);
  private static final String FALLBACK_ATLAS = "images/enemies/default.atlas";
  private static final String[] BESTIARY_TEXTURES = {BestiaryDisplay.BUTTON_TEXTURE};

  private final GdxGame game;
  private final Renderer renderer;
  private final BestiaryService bestiary;
  private final String[] atlasPaths;

  /** Creates a bestiary screen backed by the current game's discovery progress. */
  public BestiaryScreen(GdxGame game) {
    this.game = Objects.requireNonNull(game, "game cannot be null");
    this.bestiary =
        Objects.requireNonNull(game.getBestiaryService(), "Bestiary service is missing");
    this.atlasPaths = collectAtlasPaths(bestiary);

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
    ServiceLocator.getResourceService().unloadAssets(atlasPaths);
    ServiceLocator.getResourceService().unloadAssets(BESTIARY_TEXTURES);
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.clear();
  }

  private void loadAssets() {
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(BESTIARY_TEXTURES);
    resourceService.loadTextureAtlases(atlasPaths);
    resourceService.loadAll();
  }

  private void createUI() {
    Stage stage = ServiceLocator.getRenderService().getStage();
    Entity ui = new Entity();
    ui.addComponent(new BestiaryDisplay(bestiary, () -> game.setScreen(GdxGame.ScreenType.LIBRARY)))
        .addComponent(new InputDecorator(stage, 10));
    ServiceLocator.getEntityService().register(ui);
  }

  static String[] collectAtlasPaths(BestiaryService bestiary) {
    Set<String> paths = new LinkedHashSet<>();
    paths.add(FALLBACK_ATLAS);
    for (BestiaryEntryView entry : bestiary.getEntries()) {
      entry.sprite().ifPresent(paths::add);
    }
    return paths.toArray(new String[0]);
  }
}

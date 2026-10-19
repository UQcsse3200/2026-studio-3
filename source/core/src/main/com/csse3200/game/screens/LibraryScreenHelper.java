package com.csse3200.game.screens;

import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;

/** Shared service and asset operations for the card and story library screens. */
final class LibraryScreenHelper {
  private LibraryScreenHelper() {}

  /** Registers the library screen services and creates its renderer. */
  static Renderer createRenderer() {
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());
    ServiceLocator.registerTimeSource(new GameTime());
    return RenderFactory.createRenderer();
  }

  /** Loads the requested textures through the shared resource service. */
  static void loadTextures(String[] textures) {
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(textures);
    resourceService.loadAll();
  }

  /** Updates screen entities and renders the current frame. */
  static void render(Renderer renderer) {
    ServiceLocator.getEntityService().update();
    renderer.render();
  }

  /** Disposes screen services and unloads the requested textures. */
  static void dispose(Renderer renderer, String[] textures) {
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getResourceService().unloadAssets(textures);
  }
}

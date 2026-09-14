package com.csse3200.game.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.RenderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ServiceLocatorTest {
  @Test
  void shouldGetSetServices() {
    EntityService entityService = new EntityService();
    RenderService renderService = new RenderService();
    PhysicsService physicsService = mock(PhysicsService.class);
    GameTime gameTime = new GameTime();
    GamePauseService pauseService = new GamePauseService(gameTime);

    ServiceLocator.registerEntityService(entityService);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerPhysicsService(physicsService);
    ServiceLocator.registerTimeSource(gameTime);
    ServiceLocator.registerPauseService(pauseService);

    assertEquals(ServiceLocator.getEntityService(), entityService);
    assertEquals(ServiceLocator.getRenderService(), renderService);
    assertEquals(ServiceLocator.getPhysicsService(), physicsService);
    assertEquals(ServiceLocator.getTimeSource(), gameTime);
    assertEquals(ServiceLocator.getPauseService(), pauseService);

    ServiceLocator.clear();
    assertNull(ServiceLocator.getEntityService());
    assertNull(ServiceLocator.getRenderService());
    assertNull(ServiceLocator.getPhysicsService());
    assertNull(ServiceLocator.getTimeSource());
    assertNull(ServiceLocator.getPauseService());
  }
}

package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class LibraryScreenTest {
  @Test
  void initialiseServicesRegistersResourceService() {
    LibraryScreen.initialiseServices();

    assertNotNull(ServiceLocator.getResourceService());
  }
}

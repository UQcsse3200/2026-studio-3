package com.csse3200.game.areas;

import static org.junit.jupiter.api.Assertions.assertNull;

import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;

class ForestGameAreaTitleTest {
  @Test
  void battleUiDoesNotReintroduceTheOldAreaTitle() {
    EntityService entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    try {
      ForestGameArea area = new ForestGameArea(null, 1, new RunState(), "dungeon");
      Entity ui = new Entity();
      area.displayUI(ui);

      assertNull(ui.getComponent(GameAreaDisplay.class));
    } finally {
      entities.dispose();
      ServiceLocator.clear();
    }
  }
}

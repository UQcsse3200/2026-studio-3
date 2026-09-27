package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnemyIntentDisplayTest {

  private Entity entity;
  private EnemyIntentDisplay intentDisplay;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerRenderService(new RenderService());

    entity = new Entity();
    intentDisplay = new EnemyIntentDisplay();

    entity.addComponent(intentDisplay);
    intentDisplay.create();
  }

  @Test
  void shouldSetIntentWhenIntentChanged() {
    EnemyIntent intent = new EnemyIntent(IntentType.ATTACK, 2);

    entity.getEvents().trigger("intentChanged", intent);

    assertEquals(intent, intentDisplay.getCurrentIntent());
  }

  @Test
  void shouldClearIntentWhenEnemyDefeated() {
    EnemyIntent intent = new EnemyIntent(IntentType.ATTACK, 5);

    // First give the display an intent
    entity.getEvents().trigger("intentChanged", intent);
    assertEquals(intent, intentDisplay.getCurrentIntent());

    // Then defeat the enemy
    entity.getEvents().trigger("enemyDefeated");

    assertNull(intentDisplay.getCurrentIntent());
  }
}

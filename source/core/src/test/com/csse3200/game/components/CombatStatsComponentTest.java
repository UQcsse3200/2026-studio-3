package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CombatStatsComponentTest {
  @Test
  void shouldSetGetHealth() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    assertEquals(100, combat.getHealth());

    combat.setHealth(150);
    assertEquals(100, combat.getHealth());

    combat.setHealth(-50);
    assertEquals(0, combat.getHealth());
  }

  @Test
  void shouldCheckOverloadConstructor() {
    CombatStatsComponent combat = new CombatStatsComponent(150, 20, 100);
    assertEquals(100, combat.getHealth());
    combat.takeDamage(50);
    assertEquals(50, combat.getHealth());
    combat.heal(20);
    assertEquals(70, combat.getHealth());
    assertEquals(100, combat.getMaxHealth());
  }

  @Test
  void shouldCheckIsDead() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    assertFalse(combat.isDead());

    combat.setHealth(0);
    assertTrue(combat.isDead());
  }

  @Test
  void shouldAddHealth() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.addHealth(-500);
    assertEquals(0, combat.getHealth());

    combat.addHealth(100);
    combat.addHealth(-20);
    assertEquals(80, combat.getHealth());
  }

  @Test
  void shouldSetGetMaxHealth() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    assertEquals(100, combat.getMaxHealth());

    combat.setMaxHealth(150);
    assertEquals(150, combat.getMaxHealth());

    combat.setMaxHealth(-50);
    assertEquals(1, combat.getMaxHealth());
  }

  @Test
  void shouldAddMaxHealth() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.addMaxHealth(-500);
    assertEquals(1, combat.getMaxHealth());

    combat.addMaxHealth(99);
    combat.addMaxHealth(-20);
    assertEquals(80, combat.getMaxHealth());
  }

  @Test
  void shouldHeal() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.heal(-100);
    assertEquals(100, combat.getHealth());

    combat.setHealth(50);
    combat.heal(30);
    assertEquals(80, combat.getHealth());

    combat.heal(30);
    assertEquals(100, combat.getHealth());
  }

  @Test
  void shouldTakeDamage() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.takeDamage(-50);
    assertEquals(100, combat.getHealth());

    combat.takeDamage(50);
    assertEquals(50, combat.getHealth());

    combat.takeDamage(80);
    assertEquals(0, combat.getHealth());
  }

  @Test
  void shouldTriggerDeathEvent() {
    Entity entity = new Entity();
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    entity.addComponent(combat);
    EventListener0 listener = mock(EventListener0.class);
    entity.getEvents().addListener("entityIsDead", listener);
    combat.takeDamage(100);
    verify(listener).handle();
  }

  @Test
  void shouldSetGetBaseAttack() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    assertEquals(20, combat.getBaseAttack());

    combat.setBaseAttack(150);
    assertEquals(150, combat.getBaseAttack());

    combat.setBaseAttack(-50);
    assertEquals(150, combat.getBaseAttack());
  }
}

package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.maps.PlayerRunState;
import org.junit.jupiter.api.Test;

class ComponentPlayerStateAdapterTest {
  @Test
  void shouldReadAndUpdateCurrentPlayerComponents() {
    CombatStatsComponent combatStats = new CombatStatsComponent(100, 10);
    InventoryComponent inventory = new InventoryComponent(50);
    ComponentPlayerStateAdapter player = new ComponentPlayerStateAdapter(combatStats, inventory);

    assertEquals(100, player.getHealth());
    assertEquals(50, player.getCurrency());

    player.setHealth(80);
    player.setCurrency(35);

    assertEquals(80, combatStats.getHealth());
    assertEquals(35, inventory.getGold());
  }

  @Test
  void shouldDelegateDirectHealthWithoutConsumingArmourOrBlock() {
    CombatStatsComponent combatStats = new CombatStatsComponent(50, 10, 100);
    combatStats.setArmour(12);
    combatStats.setBlock(8);
    ComponentPlayerStateAdapter player =
        new ComponentPlayerStateAdapter(combatStats, new InventoryComponent(50));

    player.applyDirectHealthChange(-20);

    assertEquals(30, player.getHealth());
    assertEquals(100, player.getMaxHealth());
    assertEquals(12, combatStats.getArmour());
    assertEquals(8, combatStats.getBlock());
  }

  @Test
  void shouldPreserveDirectHealthDeathEventSemantics() {
    Entity entity = new Entity();
    CombatStatsComponent combatStats = new CombatStatsComponent(5, 10, 100);
    entity.addComponent(combatStats);
    EventListener0 deathListener = mock(EventListener0.class);
    entity.getEvents().addListener("entityIsDead", deathListener);
    ComponentPlayerStateAdapter player =
        new ComponentPlayerStateAdapter(combatStats, new InventoryComponent(50));

    player.applyDirectHealthChange(-20);
    player.applyDirectHealthChange(-5);

    assertEquals(0, player.getHealth());
    verify(deathListener, times(1)).handle();
  }

  @Test
  void shouldCaptureAppliedChanceHealthAndCurrencyIntoSharedRunState() {
    PlayerRunState runState = new PlayerRunState(95, 100, 40);
    Entity playerEntity =
        new Entity()
            .addComponent(new CombatStatsComponent(1, 10, 1))
            .addComponent(new InventoryComponent(0));
    runState.applyTo(playerEntity);
    ComponentPlayerStateAdapter player =
        new ComponentPlayerStateAdapter(
            playerEntity.getComponent(CombatStatsComponent.class),
            playerEntity.getComponent(InventoryComponent.class));

    ChanceResolution result = new ChanceOutcomeApplier(player).apply(new ChanceOutcome(20, -15));
    runState.captureFrom(playerEntity);

    assertEquals(ChanceResolution.Status.APPLIED, result.getStatus());
    assertEquals(100, runState.getCurrentHealth());
    assertEquals(25, runState.getGold());
  }
}

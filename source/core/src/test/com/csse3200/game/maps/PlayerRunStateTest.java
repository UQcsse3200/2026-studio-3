package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rewards.ItemType;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlayerRunStateTest {
  @Test
  void appliesPersistedValuesToAScreenPlayer() {
    PlayerRunState state = new PlayerRunState(65, 100, 42);
    Entity player = player(10, 20, 1);

    state.applyTo(player);

    assertEquals(65, player.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(100, player.getComponent(CombatStatsComponent.class).getMaxHealth());
    assertEquals(42, player.getComponent(InventoryComponent.class).getGold());
  }

  @Test
  void capturesValuesBeforeTheScreenPlayerIsDisposed() {
    PlayerRunState state = new PlayerRunState(65, 100, 42);
    Entity player = player(30, 120, 77);

    state.captureFrom(player);

    assertEquals(30, state.getCurrentHealth());
    assertEquals(120, state.getMaxHealth());
    assertEquals(77, state.getGold());
  }

  @Test
  void rejectsInvalidStateWithoutPartiallyChangingExistingValues() {
    PlayerRunState state = new PlayerRunState(65, 100, 42);

    assertThrows(IllegalArgumentException.class, () -> state.restore(101, 100, 10));

    assertEquals(65, state.getCurrentHealth());
    assertEquals(100, state.getMaxHealth());
    assertEquals(42, state.getGold());
  }

  private Entity player(int health, int maxHealth, int gold) {
    return new Entity()
        .addComponent(new CombatStatsComponent(health, 5, maxHealth))
        .addComponent(new InventoryComponent(gold));
  }

  @Test
  void ownedItemsSurviveCaptureAndApply() {
    PlayerRunState state = new PlayerRunState(100, 100, 50);

    state.addOwnedItem(ItemType.LUCKY_COIN);

    Entity firstPlayer =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 5, 100))
            .addComponent(new InventoryComponent(50))
            .addComponent(new EnergyComponent(3));

    state.applyTo(firstPlayer);
    state.captureFrom(firstPlayer);

    Entity secondPlayer =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 5, 100))
            .addComponent(new InventoryComponent(0))
            .addComponent(new EnergyComponent(3));

    state.applyTo(secondPlayer);

    assertEquals(List.of(ItemType.LUCKY_COIN), state.getOwnedItems());

    assertEquals(
        0.1f, secondPlayer.getComponent(InventoryComponent.class).getGoldBonusMultiplier(), 0.001f);
  }

  @Test
  void multipleLuckyCoinsStack() {
    PlayerRunState state = new PlayerRunState(100, 100, 50);

    state.addOwnedItem(ItemType.LUCKY_COIN);
    state.addOwnedItem(ItemType.LUCKY_COIN);

    assertEquals(0.2f, state.getGoldBonusMultiplier(), 0.001f);
  }
}

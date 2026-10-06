package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  @Test
  void battleConsumablesApplyOnlyWhenExplicitlyUsedAndRemoveOneOwnedCopy() {
    PlayerRunState state = new PlayerRunState(100, 100, 50);
    state.addOwnedItem(ItemType.IRON_AEGIS);
    state.addOwnedItem(ItemType.WARRIORS_CREST);
    Entity player = player(100, 100, 50);

    state.applyTo(player);
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    assertEquals(0, stats.getArmour());
    assertNull(stats.getStatusEffect("STRENGTH"));

    assertTrue(state.useBattleItem(ItemType.IRON_AEGIS, player));
    assertEquals(5, stats.getArmour());
    assertEquals(0, state.getOwnedItemCount(ItemType.IRON_AEGIS));

    assertTrue(state.useBattleItem(ItemType.WARRIORS_CREST, player));
    assertEquals(1, stats.getStatusEffect("STRENGTH").getValue());
    assertEquals(0, state.getOwnedItemCount(ItemType.WARRIORS_CREST));
    assertFalse(state.useBattleItem(ItemType.IRON_AEGIS, player));
    assertFalse(state.useBattleItem(ItemType.MERCHANTS_FAVOR, player));
  }

  @Test
  void doesNotRemoveBattleConsumableWhenItCannotBeApplied() {
    PlayerRunState state = new PlayerRunState(100, 100, 50);
    state.addOwnedItem(ItemType.IRON_AEGIS);

    assertThrows(
        IllegalArgumentException.class, () -> state.useBattleItem(ItemType.IRON_AEGIS, new Entity()));

    assertEquals(1, state.getOwnedItemCount(ItemType.IRON_AEGIS));
  }

  @Test
  void merchantsFavorGivesTenPercentPerCopyAndCapsAtFiftyPercent() {
    PlayerRunState state = new PlayerRunState(100, 100, 50);
    for (int i = 0; i < 6; i++) {
      state.addOwnedItem(ItemType.MERCHANTS_FAVOR);
    }

    Entity player = player(100, 100, 50);
    state.applyTo(player);

    assertEquals(0.5f, state.getShopDiscount(), 0.001f);
    assertEquals(0.5f, player.getComponent(InventoryComponent.class).getShopDiscount(), 0.001f);
  }
}

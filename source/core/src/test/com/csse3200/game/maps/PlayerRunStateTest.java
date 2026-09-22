package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rewards.ItemType;
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

  private Entity playerWithEnergy(int health, int maxHealth, int gold, int maxEnergy) {
    return new Entity()
        .addComponent(new CombatStatsComponent(health, 5, maxHealth))
        .addComponent(new InventoryComponent(gold))
        .addComponent(new EnergyComponent(maxEnergy));
  }

  @Test
  void ownedItemEffectPersistsAcrossMultipleBattles() {
    // Regression test: previously RunState.pendingReward only reapplied the single
    // most-recently-claimed reward, so an item's effect (e.g. shop discount) was lost
    // as soon as the next battle after that one began. Owned items must now be
    // replayed on every new player entity, not just the one immediately following
    // the claim.
    PlayerRunState state = new PlayerRunState(65, 100, 42);
    state.addOwnedItem(ItemType.MERCHANTS_FAVOR);

    // Battle 1: new player entity, as BattleScreen creates on each screen transition.
    Entity battleOnePlayer = player(65, 100, 42);
    state.applyTo(battleOnePlayer);
    assertEquals(
        0.05f, battleOnePlayer.getComponent(InventoryComponent.class).getShopDiscount(), 1e-6f);

    // Battle 2: a completely new player entity/components, same as after disposing
    // battle 1's screen. The discount must still be there.
    Entity battleTwoPlayer = player(65, 100, 42);
    state.applyTo(battleTwoPlayer);
    assertEquals(
        0.05f, battleTwoPlayer.getComponent(InventoryComponent.class).getShopDiscount(), 1e-6f);
  }

  @Test
  void multipleOwnedItemsStackOnEachNewPlayer() {
    PlayerRunState state = new PlayerRunState(65, 100, 42);
    state.addOwnedItem(ItemType.MERCHANTS_FAVOR);
    state.addOwnedItem(ItemType.MERCHANTS_FAVOR);
    state.addOwnedItem(ItemType.ENERGY_CRYSTAL);

    Entity player = playerWithEnergy(65, 100, 42, 3);
    state.applyTo(player);

    assertEquals(0.10f, player.getComponent(InventoryComponent.class).getShopDiscount(), 1e-6f);
    assertEquals(4, player.getComponent(EnergyComponent.class).getMaxEnergy());
  }

  @Test
  void shopDiscountStillRespectsCapAcrossManyOwnedItems() {
    PlayerRunState state = new PlayerRunState(65, 100, 42);
    for (int i = 0; i < 20; i++) {
      state.addOwnedItem(ItemType.MERCHANTS_FAVOR);
    }

    Entity player = player(65, 100, 42);
    state.applyTo(player);

    assertEquals(0.5f, player.getComponent(InventoryComponent.class).getShopDiscount(), 1e-6f);
  }

  @Test
  void getOwnedItemsReturnsImmutableView() {
    PlayerRunState state = new PlayerRunState(65, 100, 42);
    state.addOwnedItem(ItemType.LUCKY_COIN);

    var ownedItems = state.getOwnedItems();

    assertThrows(
        UnsupportedOperationException.class, () -> ownedItems.add(ItemType.ENERGY_CRYSTAL));
  }
}

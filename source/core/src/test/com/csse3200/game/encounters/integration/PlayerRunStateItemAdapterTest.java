package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.rewards.ItemType;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlayerRunStateItemAdapterTest {
  @Test
  void shouldUseEveryAvailableItemDefinitionWithoutApplyingEffectsToLivePlayer() {
    // Also exercises Team 7's Curse IDs when this test runs in the isolated dependency tree.
    for (ItemType item : ItemType.values()) {
      PlayerRunState state = new PlayerRunState(35, 100, 42);
      Entity live =
          new Entity()
              .addComponent(new CombatStatsComponent(35, 5, 100))
              .addComponent(new InventoryComponent(42))
              .addComponent(new EnergyComponent(3));
      PlayerRunStateItemAdapter adapter = new PlayerRunStateItemAdapter(state);

      assertTrue(adapter.containsItem(item.name()));
      assertTrue(adapter.addItem(item.name()));
      adapter.commitItemAddition(item.name());
      assertEquals(List.of(item), state.getOwnedItems());
      assertEquals(35, state.getCurrentHealth());
      assertEquals(100, state.getMaxHealth());
      assertEquals(42, state.getGold());
      assertEquals(3, live.getComponent(EnergyComponent.class).getMaxEnergy());
      assertEquals(100, live.getComponent(CombatStatsComponent.class).getMaxHealth());
      state.captureFrom(live);
      assertEquals(List.of(item), state.getOwnedItems());
    }
  }

  @Test
  void shouldRollbackOnlyNewCopiesAndPreservePreviousInventoryOrder() {
    PlayerRunState state = new PlayerRunState(35, 100, 42);
    List<ItemType> original = List.of(ItemType.ENERGY_CRYSTAL, ItemType.MERCHANTS_FAVOR);
    state.restoreOwnedItems(original);
    PlayerRunStateItemAdapter adapter = new PlayerRunStateItemAdapter(state);
    assertTrue(adapter.addItem("ENERGY_CRYSTAL"));
    assertTrue(adapter.addItem("ENERGY_CRYSTAL"));

    assertTrue(adapter.rollbackItemAddition("ENERGY_CRYSTAL"));
    assertTrue(adapter.rollbackItemAddition("ENERGY_CRYSTAL"));
    assertEquals(original, state.getOwnedItems());
    assertFalse(adapter.rollbackItemAddition("ENERGY_CRYSTAL"));
  }

  @Test
  void shouldNotOverwriteAnInventoryChangedByAnotherWriter() {
    PlayerRunState state = new PlayerRunState(35, 100, 42);
    PlayerRunStateItemAdapter adapter = new PlayerRunStateItemAdapter(state);
    adapter.addItem("ENERGY_CRYSTAL");
    state.addOwnedItem(ItemType.LUCKY_COIN);

    assertFalse(adapter.rollbackItemAddition("ENERGY_CRYSTAL"));
    assertEquals(List.of(ItemType.ENERGY_CRYSTAL, ItemType.LUCKY_COIN), state.getOwnedItems());
  }

  @Test
  void shouldValidateAuthoritativeIdsAndDiscardCommittedRollbackMetadata() {
    PlayerRunState state = new PlayerRunState(35, 100, 42);
    PlayerRunStateItemAdapter adapter = new PlayerRunStateItemAdapter(state);
    assertFalse(adapter.containsItem(null));
    assertFalse(adapter.containsItem(""));
    assertFalse(adapter.containsItem("energy_crystal"));
    assertFalse(adapter.containsItem("UNKNOWN_ITEM"));
    adapter.addItem("LUCKY_COIN");
    adapter.commitItemAddition("LUCKY_COIN");
    assertFalse(adapter.rollbackItemAddition("LUCKY_COIN"));
    assertEquals(1, state.getOwnedItemCount(ItemType.LUCKY_COIN));
  }
}

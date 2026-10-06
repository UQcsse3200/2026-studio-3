package com.csse3200.game.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.csse3200.game.bestiary.BestiaryService;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.deck.PlayerDeckFactory;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rewards.ItemType;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/** Verifies item-derived player effects survive a real save file and process relaunch. */
@ExtendWith(GameExtension.class)
class PlayerItemEffectsSaveTest {

  @TempDir Path temporaryDirectory;

  @Test
  void shouldRestoreOwnedItemsAndTheirEffectsAfterRelaunch() {
    PlayerRunState originalPlayer = new PlayerRunState(50, 80, 25);
    originalPlayer.addOwnedItem(ItemType.LUCKY_COIN);
    originalPlayer.addOwnedItem(ItemType.ENERGY_CRYSTAL);
    originalPlayer.addOwnedItem(ItemType.MERCHANTS_FAVOR);
    originalPlayer.addOwnedItem(ItemType.IRON_AEGIS);
    originalPlayer.addOwnedItem(ItemType.WARRIORS_CREST);

    SaveGameData loaded = saveAndLoad(originalPlayer);
    PlayerRunState restoredPlayer = restoreAfterRelaunch(loaded);
    Entity player = playerEntity();
    restoredPlayer.applyTo(player);

    assertEquals(
        List.of(
            ItemType.LUCKY_COIN,
            ItemType.ENERGY_CRYSTAL,
            ItemType.MERCHANTS_FAVOR,
            ItemType.IRON_AEGIS,
            ItemType.WARRIORS_CREST),
        restoredPlayer.getOwnedItems());
    assertEquals(
        0.1f, player.getComponent(InventoryComponent.class).getGoldBonusMultiplier(), 0.001f);
    assertEquals(0.05f, player.getComponent(InventoryComponent.class).getShopDiscount(), 0.001f);
    assertEquals(4, player.getComponent(EnergyComponent.class).getMaxEnergy());
  }

  @Test
  void shouldTreatMissingOwnedItemsAsEmptyForOldSaves() {
    SaveGameData loaded = saveAndLoad(new PlayerRunState(50, 80, 25));
    loaded.player.ownedItems = null;

    PlayerRunState restoredPlayer = restoreAfterRelaunch(loaded);
    Entity player = playerEntity();
    restoredPlayer.applyTo(player);

    assertTrue(restoredPlayer.getOwnedItems().isEmpty());
    assertEquals(0f, player.getComponent(InventoryComponent.class).getGoldBonusMultiplier());
    assertEquals(0f, player.getComponent(InventoryComponent.class).getShopDiscount());
    assertEquals(3, player.getComponent(EnergyComponent.class).getMaxEnergy());
  }

  private SaveGameData saveAndLoad(PlayerRunState playerState) {
    RunState runState = new RunState();
    runState.startRun(oneNodeMap(), 0);
    SaveGameService saveGameService =
        new SaveGameService(
            repository(),
            new GameStateSnapshotProvider(
                playerState,
                PlayerDeckFactory.createStarterDeck(),
                runState,
                BestiaryService.loadDefault(),
                CardDiscoveryService.loadDefault()));

    assertTrue(saveGameService.saveGame(1).success());
    LoadResult loaded = new SaveGameService(repository()).loadGame(1);
    assertTrue(loaded.success());
    return loaded.data();
  }

  private PlayerRunState restoreAfterRelaunch(SaveGameData data) {
    PlayerRunState restoredPlayer = new PlayerRunState(1, 10, 0);
    PlayerDeck restoredDeck = PlayerDeckFactory.createStarterDeck();
    restoredDeck.clear();
    RestoreResult result =
        new SaveGameRestoreService(
                restoredPlayer,
                restoredDeck,
                new RunState(),
                BestiaryService.loadDefault(),
                CardDiscoveryService.loadDefault())
            .restore(data);

    assertTrue(result.success());
    return restoredPlayer;
  }

  private JsonSaveGameRepository repository() {
    return new JsonSaveGameRepository(new FileHandle(temporaryDirectory.toFile()));
  }

  private static Entity playerEntity() {
    return new Entity()
        .addComponent(new CombatStatsComponent(1, 5, 10))
        .addComponent(new InventoryComponent(0))
        .addComponent(new EnergyComponent(3));
  }

  private static MapGraph oneNodeMap() {
    Map<Integer, MapNode> nodes = new HashMap<>();
    nodes.put(0, new MapNode(0, RoomType.COMBAT));
    return new MapGraph(nodes, false);
  }
}

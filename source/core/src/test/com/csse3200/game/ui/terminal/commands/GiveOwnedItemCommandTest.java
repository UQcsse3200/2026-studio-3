package com.csse3200.game.ui.terminal.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.rewards.ItemAcquisitionService;
import com.csse3200.game.rewards.ItemDiscoveryService;
import com.csse3200.game.rewards.ItemType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GiveOwnedItemCommandTest {

  @Test
  void addsKnownItemsToTheRunInventoryAndAllowsStacking() {
    PlayerRunState playerState = new PlayerRunState(100, 100, 0);
    ItemDiscoveryService discovery = new ItemDiscoveryService();
    GiveOwnedItemCommand command =
        new GiveOwnedItemCommand(playerState, new ItemAcquisitionService(discovery));
    ArrayList<String> args = new ArrayList<>(List.of("ASHEN_EMBER"));

    assertTrue(command.action(args));
    assertTrue(command.action(args));

    assertEquals(2, playerState.getOwnedItemCount(ItemType.ASHEN_EMBER));
    assertFalse(discovery.isDiscovered(ItemType.ASHEN_EMBER));
  }

  @Test
  void rejectsUnknownItemsWithoutChangingTheInventory() {
    PlayerRunState playerState = new PlayerRunState(100, 100, 0);
    GiveOwnedItemCommand command =
        new GiveOwnedItemCommand(
            playerState, new ItemAcquisitionService(new ItemDiscoveryService()));

    assertFalse(command.action(new ArrayList<>(List.of("NOT_AN_ITEM"))));
    assertTrue(playerState.getOwnedItems().isEmpty());
  }

  @Test
  void rejectsMissingPlayerState() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new GiveOwnedItemCommand(null, new ItemAcquisitionService(new ItemDiscoveryService())));
  }

  @Test
  void rejectsMissingAcquisitionService() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GiveOwnedItemCommand(new PlayerRunState(100, 100, 0), null));
  }
}
